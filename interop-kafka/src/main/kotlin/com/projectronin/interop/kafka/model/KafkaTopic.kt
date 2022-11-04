package com.projectronin.interop.kafka.model

import com.projectronin.interop.kafka.config.KafkaCloudConfig

/**
 * A KafkaTopic is a representation of a topic as defined by the Ronin standards.
 */
interface KafkaTopic {
    val tenantSpecific: Boolean
    val systemName: String
    val eventType: String
    val version: String
    val dataSchema: String

    /**
     * Returns the topic name for this topic when implemented against the [cloudConfig] and [tenantId].
     */
    fun getTopicName(cloudConfig: KafkaCloudConfig, tenantId: String, trigger: DataTrigger?): String {
        val system = if (tenantSpecific) "$systemName-$tenantId" else systemName
        val event = trigger?.let { "$eventType-${it.type}" } ?: eventType

        return "${cloudConfig.vendor}.${cloudConfig.region}.$system.$event.$version"
    }
}
