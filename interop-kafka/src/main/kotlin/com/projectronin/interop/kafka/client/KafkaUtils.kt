package com.projectronin.interop.kafka.client

import com.projectronin.interop.kafka.model.KafkaTopic
import com.projectronin.interop.kafka.spring.KafkaConfig
import com.projectronin.kafka.RoninConsumer
import com.projectronin.kafka.RoninProducer
import com.projectronin.kafka.config.RoninConsumerKafkaProperties
import com.projectronin.kafka.config.RoninProducerKafkaProperties
import kotlin.reflect.KClass

/**
 * Creates a [RoninProducer] capable of publishing to the [KafkaTopic] represented by [topic].
 */
fun createProducer(
    topic: KafkaTopic,
    kafkaConfig: KafkaConfig
): RoninProducer {
    val kafkaProperties = kafkaConfig.properties
    val producerProperties = RoninProducerKafkaProperties(
        "bootstrap.servers" to kafkaConfig.bootstrap.servers,
        "security.protocol" to kafkaProperties.security.protocol,
        "sasl.mechanism" to kafkaProperties.sasl.mechanism,
        "sasl.jaas.config" to kafkaProperties.sasl.jaas.config
    )
    return RoninProducer(
        topic = topic.topicName,
        source = kafkaConfig.publish.source,
        dataSchema = topic.dataSchema,
        kafkaProperties = producerProperties
    )
}

/**
 * Creates a [RoninConsumer] capable of publishing to the [KafkaTopic] represented by [topic],
 * allows for consumer group to be overridden with [overriddenGroupId].
 */
fun createConsumer(
    topic: KafkaTopic,
    typeMap: Map<String, KClass<*>>,
    kafkaConfig: KafkaConfig,
    overriddenGroupId: String? = null
): RoninConsumer {
    val kafkaProperties = kafkaConfig.properties
    val consumerProperties = RoninConsumerKafkaProperties(
        "bootstrap.servers" to kafkaConfig.bootstrap.servers,
        "security.protocol" to kafkaProperties.security.protocol,
        "sasl.mechanism" to kafkaProperties.sasl.mechanism,
        "sasl.jaas.config" to kafkaProperties.sasl.jaas.config,
        "group.id" to (overriddenGroupId ?: kafkaConfig.retrieve.groupId)
    )
    return RoninConsumer(
        topics = listOf(topic.topicName),
        typeMap = typeMap,
        kafkaProperties = consumerProperties
    )
}
