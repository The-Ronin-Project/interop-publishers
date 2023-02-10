package com.projectronin.interop.kafka

import com.projectronin.event.interop.resource.load.v1.InteropResourceLoadV1
import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.Failure
import com.projectronin.interop.kafka.model.KafkaAction
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.model.LoadTopic
import com.projectronin.interop.kafka.model.PushResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Service responsible for creating load events to Kafka
 */
@Service
class KafkaLoadService(private val kafkaClient: KafkaClient, topics: List<LoadTopic>) {
    private val logger = KotlinLogging.logger { }

    private val loadTopicsByResourceType = topics.groupBy { it.resourceType.lowercase() }

    /**
     * Triggers a Load event for the [resourceFHIRIds] to the appropriate Kafka topics for [tenantId].
     */
    fun pushLoadEvent(
        tenantId: String,
        trigger: DataTrigger,
        resourceFHIRIds: List<String>,
        resourceType: ResourceType
    ): PushResponse<String> {
        val loadTopic = getTopic(resourceType)
        if (loadTopic == null) {
            logger.error { "No matching LoadTopics associated to resource type $resourceType" }
            return PushResponse(
                failures = resourceFHIRIds.map {
                    Failure(
                        it,
                        IllegalStateException("Zero or multiple LoadTopics associated to resource type $resourceType")
                    )
                }
            )
        } else {
            val events = resourceFHIRIds.map {
                KafkaEvent(
                    domain = loadTopic.systemName,
                    resource = "resource",
                    action = KafkaAction.LOAD,
                    resourceId = it,
                    data = InteropResourceLoadV1(
                        tenantId = tenantId,
                        resourceFHIRId = it,
                        resourceType = resourceType.name,
                        dataTrigger = when (trigger) {
                            DataTrigger.AD_HOC -> InteropResourceLoadV1.DataTrigger.adhoc
                            DataTrigger.NIGHTLY -> InteropResourceLoadV1.DataTrigger.nightly
                        }
                    )
                )
            }

            return runCatching { kafkaClient.publishEvents(loadTopic, events) }.fold(
                onSuccess = { response ->
                    PushResponse(
                        successful = response.successful.map { it.data.resourceFHIRId },
                        failures = response.failures.map { Failure(it.data.data.resourceFHIRId, it.error) }
                    )
                },
                onFailure = { exception ->
                    logger.error(exception) { "Exception while attempting to publish events to $loadTopic" }
                    PushResponse(
                        failures = events.map { Failure(it.data.resourceFHIRId, exception) }
                    )
                }
            )
        }
    }

    fun retrieveLoadEvents(resourceType: ResourceType, groupId: String? = null): List<InteropResourceLoadV1> {
        val topic = getTopic(resourceType) ?: return emptyList()
        val typeMap = mapOf("ronin.interop-platform.resource.load" to InteropResourceLoadV1::class)
        val events = kafkaClient.retrieveEvents(topic, typeMap, groupId)
        return events.map {
            it.data as InteropResourceLoadV1
        }
    }

    fun getTopic(resourceType: ResourceType): LoadTopic? {
        return loadTopicsByResourceType[resourceType.name.lowercase()]?.singleOrNull()
    }
}
