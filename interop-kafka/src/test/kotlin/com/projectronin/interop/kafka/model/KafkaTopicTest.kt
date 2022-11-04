package com.projectronin.interop.kafka.model

import com.projectronin.interop.kafka.config.KafkaCloudConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KafkaTopicTest {
    @Test
    fun `generates tenant-specific topic name with trigger`() {
        val topic = object : KafkaTopic {
            override val tenantSpecific: Boolean = true
            override val systemName: String = "system"
            override val eventType: String = "event"
            override val version: String = "v1"
            override val dataSchema: String = "dataSchema"
        }
        val cloudConfig = KafkaCloudConfig(vendor = "oci", region = "us-phoenix-1")
        val tenantId = "test"

        val topicName = topic.getTopicName(cloudConfig, tenantId, DataTrigger.NIGHTLY)
        assertEquals("oci.us-phoenix-1.system-test.event-nightly.v1", topicName)
    }

    @Test
    fun `generates tenant-specific topic name without trigger`() {
        val topic = object : KafkaTopic {
            override val tenantSpecific: Boolean = true
            override val systemName: String = "system"
            override val eventType: String = "event"
            override val version: String = "v1"
            override val dataSchema: String = "dataSchema"
        }
        val cloudConfig = KafkaCloudConfig(vendor = "oci", region = "us-phoenix-1")
        val tenantId = "test"

        val topicName = topic.getTopicName(cloudConfig, tenantId, null)
        assertEquals("oci.us-phoenix-1.system-test.event.v1", topicName)
    }

    @Test
    fun `generates non-tenant-specific topic name with trigger`() {
        val topic = object : KafkaTopic {
            override val tenantSpecific: Boolean = false
            override val systemName: String = "system"
            override val eventType: String = "event"
            override val version: String = "v1"
            override val dataSchema: String = "dataSchema"
        }
        val cloudConfig = KafkaCloudConfig(vendor = "oci", region = "us-phoenix-1")
        val tenantId = "test"

        val topicName = topic.getTopicName(cloudConfig, tenantId, DataTrigger.AD_HOC)
        assertEquals("oci.us-phoenix-1.system.event-adhoc.v1", topicName)
    }

    @Test
    fun `generates non-tenant-specific topic name without trigger`() {
        val topic = object : KafkaTopic {
            override val tenantSpecific: Boolean = false
            override val systemName: String = "system"
            override val eventType: String = "event"
            override val version: String = "v1"
            override val dataSchema: String = "dataSchema"
        }
        val cloudConfig = KafkaCloudConfig(vendor = "oci", region = "us-phoenix-1")
        val tenantId = "test"

        val topicName = topic.getTopicName(cloudConfig, tenantId, null)
        assertEquals("oci.us-phoenix-1.system.event.v1", topicName)
    }
}
