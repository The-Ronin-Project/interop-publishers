package com.projectronin.interop.kafka.client

import com.projectronin.interop.kafka.model.Failure
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.model.KafkaTopic
import com.projectronin.interop.kafka.model.PushResponse
import com.projectronin.interop.kafka.spring.KafkaConfig
import com.projectronin.kafka.RoninProducer
import com.projectronin.kafka.data.RoninEvent
import com.projectronin.kafka.data.RoninEventResult
import org.springframework.stereotype.Component
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.reflect.KClass

/**
 * Client for managing communication with Kafka.
 */
@Component
class KafkaClient(private val kafkaConfig: KafkaConfig) {
    private val producersByTopicName: MutableMap<String, RoninProducer> = mutableMapOf()

    /**
     * Publishes the [events] to the Kafka [topic] on behalf of a tenant.
     */
    fun <T> publishEvents(
        topic: KafkaTopic,
        events: List<KafkaEvent<T>>
    ): PushResponse<KafkaEvent<T>> {
        val producer = producersByTopicName.computeIfAbsent(topic.topicName) { createProducer(topic, kafkaConfig) }

        val results = events.associateWith { event -> producer.send(event.type, event.subject, event.data) }
            .map { (event, future) ->
                runCatching { future.get() }.fold(
                    onSuccess = { Pair(event, null) },
                    onFailure = { Pair(null, Failure(event, it)) }
                )
            }
        return PushResponse(
            successful = results.mapNotNull { it.first },
            failures = results.mapNotNull { it.second }
        )
    }

    fun retrieveEvents(
        topic: KafkaTopic,
        typeMap: Map<String, KClass<*>>,
    ): List<RoninEvent<*>> {
        val messageList = mutableListOf<RoninEvent<*>>()
        val consumer = createConsumer(topic, typeMap, kafkaConfig)
        Timer("poll").schedule(5000) {
            consumer.stop() // stop processing if 5 seconds have passed
        }
        consumer.process {
            messageList.add(it)
            RoninEventResult.ACK
        }
        consumer.unsubscribe()
        return messageList
    }
}
