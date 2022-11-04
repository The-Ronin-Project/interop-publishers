package com.projectronin.interop.kafka

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.Failure
import com.projectronin.interop.kafka.model.KafkaAction
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.model.PublishResponse
import com.projectronin.interop.kafka.model.PublishTopic
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Service responsible for publishing various events to Kafka.
 */
@Service
class KafkaPublishService(private val kafkaClient: KafkaClient, topics: List<PublishTopic>) {
    private val logger = KotlinLogging.logger { }

    private val publishTopicsByResourceType = topics.groupBy { it.resourceType.lowercase() }

    /**
     * Publishes the [resources] to the appropriate Kafka topics for [tenantId].
     */
    fun publishResources(
        tenantId: String,
        trigger: DataTrigger,
        resources: List<Resource<*>>
    ): PublishResponse<Resource<*>> {
        val resourcesByType = resources.groupBy { it.resourceType.lowercase() }
        val results = resourcesByType.map { (type, resources) ->
            val publishTopic = publishTopicsByResourceType[type]?.singleOrNull()
            if (publishTopic == null) {
                logger.error { "Zero or multiple PublishTopics associated to resource type $type" }
                PublishResponse(
                    failures = resources.map {
                        Failure(
                            it,
                            IllegalStateException("Zero or multiple PublishTopics associated to resource type $type")
                        )
                    }
                )
            } else {
                val events = resources.associateBy {
                    KafkaEvent(
                        domain = publishTopic.systemName,
                        resource = type,
                        action = KafkaAction.PUBLISH,
                        resourceId = it.id!!.value,
                        data = publishTopic.converter(tenantId, it)
                    )
                }

                runCatching { kafkaClient.publishEvents(publishTopic, tenantId, trigger, events.keys.toList()) }.fold(
                    onSuccess = { response ->
                        PublishResponse(
                            successful = response.successful.map { events[it]!! },
                            failures = response.failures.map { Failure(events[it.data]!!, it.error) }
                        )
                    },
                    onFailure = { exception ->
                        logger.error(exception) { "Exception while attempting to publish events to $publishTopic" }
                        PublishResponse(
                            failures = events.map { Failure(it.value, exception) }
                        )
                    }
                )
            }
        }
        return PublishResponse(
            successful = results.flatMap { it.successful },
            failures = results.flatMap { it.failures }
        )
    }
}
