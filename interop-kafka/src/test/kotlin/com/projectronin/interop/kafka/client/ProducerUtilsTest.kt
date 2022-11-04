package com.projectronin.interop.kafka.client

import com.projectronin.interop.kafka.config.KafkaBootstrapConfig
import com.projectronin.interop.kafka.config.KafkaCloudConfig
import com.projectronin.interop.kafka.config.KafkaConfig
import com.projectronin.interop.kafka.config.KafkaPropertiesConfig
import com.projectronin.interop.kafka.config.KafkaPublishConfig
import com.projectronin.interop.kafka.config.KafkaSaslConfig
import com.projectronin.interop.kafka.config.KafkaSaslJaasConfig
import com.projectronin.interop.kafka.config.KafkaSecurityConfig
import com.projectronin.interop.kafka.model.KafkaTopic
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ProducerUtilsTest {
    @Test
    fun `createProducer works`() {
        // This test creates an actual RoninProducer, which means that it also creates a KafkaProducer
        val kafkaConfig = KafkaConfig(
            cloud = KafkaCloudConfig(
                vendor = "local",
                region = "local"
            ),
            bootstrap = KafkaBootstrapConfig(servers = "localhost:9092"),
            publish = KafkaPublishConfig(source = "interop-kafka-it"),
            properties = KafkaPropertiesConfig(
                security = KafkaSecurityConfig(protocol = "PLAINTEXT"),
                sasl = KafkaSaslConfig(
                    mechanism = "GSSAPI",
                    jaas = KafkaSaslJaasConfig(config = "")
                )
            )
        )

        val topic = mockk<KafkaTopic> {
            every { dataSchema } returns "test.topic.name.schema"
        }
        val producer = createProducer("topic name", topic, kafkaConfig)
        Assertions.assertNotNull(producer)
    }
}
