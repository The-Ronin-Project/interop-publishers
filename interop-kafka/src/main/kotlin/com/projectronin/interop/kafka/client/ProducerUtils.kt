package com.projectronin.interop.kafka.client

import com.projectronin.interop.kafka.config.KafkaConfig
import com.projectronin.interop.kafka.model.KafkaTopic
import com.projectronin.kafka.RoninProducer
import com.projectronin.kafka.config.RoninProducerKafkaProperties

/**
 * Creates a [RoninProducer] capable of publishing to the Kafka [topic] represented by [topicName].
 */
fun createProducer(topicName: String, topic: KafkaTopic, kafkaConfig: KafkaConfig): RoninProducer {
    val kafkaProperties = kafkaConfig.properties
    val producerProperties = RoninProducerKafkaProperties(
        "bootstrap.servers" to kafkaConfig.bootstrap.servers,
        "security.protocol" to kafkaProperties.security.protocol,
        "sasl.mechanism" to kafkaProperties.sasl.mechanism,
        "sasl.jaas.config" to kafkaProperties.sasl.jaas.config
    )
    return RoninProducer(
        topic = topicName,
        source = kafkaConfig.publish.source,
        dataSchema = topic.dataSchema,
        kafkaProperties = producerProperties
    )
}
