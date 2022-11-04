package com.projectronin.interop.kafka.client.config

import com.projectronin.interop.kafka.config.KafkaConfig
import com.projectronin.interop.kafka.config.SpringConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import

class SpringConfigTest {
    @Test
    fun `can build KafkaConfig with minimum required data`() {
        runApplication(
            mapOf(
                "kafka.cloud.vendor" to "oci",
                "kafka.cloud.region" to "us-phoenix-1",
                "kafka.bootstrap.servers" to "localhost:9092",
                "kafka.publish.source" to "interop-kafka-test"
            )
        )
    }

    @Test
    fun `can build KafkaConfig with additional properties`() {
        // This test is comprehensive as of 11/2, but there is no expectation that this will continue to be so.
        runApplication(
            mapOf(
                "kafka.cloud.vendor" to "oci",
                "kafka.cloud.region" to "us-phoenix-1",
                "kafka.bootstrap.servers" to "localhost:9092",
                "kafka.publish.source" to "interop-kafka-test",
                "kafka.properties.security.protocol" to "SASL_SSL",
                "kafka.properties.sasl.mechanism" to "SCRAM-SHA-512",
                "kafka.properties.sasl.username" to "saslUsername",
                "kafka.properties.sasl.password" to "saslPassword",
                "kafka.properties.sasl.jaas.config" to "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"\${kafka.sasl.username}\" password=\"\${kafka.sasl.password}\";"
            )
        )
    }

    private fun runApplication(properties: Map<String, String>) {
        val application = SpringApplication(TestApplication::class.java)
        application.setDefaultProperties(properties)
        application.run()
    }

    // This application will cause failures if KafkaConfig can not be properly created.
    @SpringBootApplication
    @Import(SpringConfig::class)
    class TestApplication(kafkaConfig: KafkaConfig) {
        fun commandLineRunner(ctx: ApplicationContext) = CommandLineRunner { args ->
        }
    }
}
