package com.projectronin.interop.publishers

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.ModificationType
import com.projectronin.event.interop.internal.v1.Metadata
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.kafka.KafkaPublishService
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.publishers.exception.PublishException
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

/**
 * Service managing the publication of data to one or more downstream repositories.
 */
@Service
class PublishService(
    private val ehrDataAuthorityClient: EHRDataAuthorityClient,
    private val datalakePublishService: DatalakePublishService,
    private val kafkaPublishService: KafkaPublishService
) {
    /**
     * Publishes the supplied [resources]. If all resources were successfully published,
     * true will be returned.  Otherwise, [PublishException] will be thrown.
     */
    fun publishFHIRResources(
        tenantId: String,
        resources: List<Resource<*>>,
        metadata: Metadata,
        dataTrigger: DataTrigger? = null
    ): Boolean {
        val resourcesById = resources.associateBy { it.id!!.value }
        val response = runBlocking { ehrDataAuthorityClient.addResources(tenantId, resources) }

        val succeeded = response.succeeded
        if (succeeded.isNotEmpty()) {
            // Publish only modified resources to Datalake
            val modifiedSuccessfulResources = succeeded.filterNot { it.modificationType == ModificationType.UNMODIFIED }
                .map { resourcesById[it.resourceId]!! }
            if (modifiedSuccessfulResources.isNotEmpty()) {
                datalakePublishService.publishFHIRR4(tenantId, modifiedSuccessfulResources)
            }

            // Publish all resources to Kafka, if a data trigger was provided
            val allSuccessfulResources = succeeded.map { resourcesById[it.resourceId]!! }
            dataTrigger?.let {
                kafkaPublishService.publishResources(
                    tenantId,
                    dataTrigger,
                    allSuccessfulResources,
                    metadata
                )
            }
        }

        val failedResources = response.failed
        if (failedResources.isNotEmpty()) {
            throw PublishException(
                "Published ${succeeded.size} resources, but failed to publish ${failedResources.size} resources: \n${
                failedResources.joinToString(
                    "\n"
                ) { "${it.resourceType}/${it.resourceId}: ${it.error}" }
                }"
            )
        }
        return true
    }
}
