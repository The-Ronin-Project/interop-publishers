package com.projectronin.interop.kafka.model

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * A PublishTopic is a [KafkaTopic] associated to a publish workflow within Interops.
 */
data class PublishTopic(
    override val tenantSpecific: Boolean,
    override val systemName: String,
    override val eventType: String,
    override val version: String,
    override val dataSchema: String,
    val resourceType: String,
    val converter: (String, Resource<*>) -> Any
) : KafkaTopic
