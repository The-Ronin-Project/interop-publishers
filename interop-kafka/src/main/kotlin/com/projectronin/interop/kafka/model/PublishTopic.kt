package com.projectronin.interop.kafka.model

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * A PublishTopic is a [KafkaTopic] associated to a publish workflow within Interops.
 */
data class PublishTopic(
    override val systemName: String,
    override val topicName: String,
    override val dataSchema: String,
    val resourceType: String,
    val dataTrigger: DataTrigger,
    val converter: (String, Resource<*>) -> Any
) : KafkaTopic
