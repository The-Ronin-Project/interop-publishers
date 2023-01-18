package com.projectronin.interop.kafka.client

import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.Failure
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.model.KafkaTopic
import com.projectronin.interop.kafka.model.PublishResponse
import com.projectronin.interop.kafka.spring.KafkaConfig
import com.projectronin.kafka.RoninProducer
import org.springframework.stereotype.Component

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
        tenantId: String,
        trigger: DataTrigger?,
        events: List<KafkaEvent<T>>
    ): PublishResponse<KafkaEvent<T>> {
        val producer = producersByTopicName.computeIfAbsent(topic.topicName) { createProducer(topic, kafkaConfig) }

        val results = events.associateWith { event -> producer.send(event.type, event.subject, event.data) }
            .map { (event, future) ->
                runCatching { future.get() }.fold(
                    onSuccess = { Pair(event, null) },
                    onFailure = { Pair(null, Failure(event, it)) }
                )
            }
        return PublishResponse(
            successful = results.mapNotNull { it.first },
            failures = results.mapNotNull { it.second }
        )
    }
}
