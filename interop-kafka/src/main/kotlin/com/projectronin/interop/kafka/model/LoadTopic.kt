package com.projectronin.interop.kafka.model

/**
 * A LoadTopic is a [KafkaTopic] associated to a load workflow within Interops.
 */
data class LoadTopic(
    override val systemName: String,
    override val topicName: String,
    override val dataSchema: String,
    val resourceType: String
) : KafkaTopic
