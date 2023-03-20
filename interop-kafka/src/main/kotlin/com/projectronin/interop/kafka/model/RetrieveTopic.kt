package com.projectronin.interop.kafka.model

/**
 * A RetrieveTopic is a [KafkaTopic] associated to a retrieval workflow within Interops.
 */
data class RetrieveTopic(
    override val systemName: String,
    override val topicName: String,
    override val dataSchema: String,
    val resourceType: String
) : KafkaTopic
