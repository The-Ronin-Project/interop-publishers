package com.projectronin.interop.publishers

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.ModificationType
import com.projectronin.event.interop.internal.v1.Metadata
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.kafka.KafkaPublishService
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.PublishResourceWrapper
import com.projectronin.interop.publishers.exception.PublishException
import com.projectronin.interop.publishers.model.PublishResponse
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

/**
 * Service managing the publication of data to one or more downstream repositories.
 */
@Service
class PublishService(
    private val ehrDataAuthorityClient: EHRDataAuthorityClient,
    private val datalakePublishService: DatalakePublishService,
    private val kafkaPublishService: KafkaPublishService,
) {
    /**
     * Publishes the supplied [resourceWrappers]. If all resources were successfully published,
     * true will be returned.  Otherwise, [PublishException] will be thrown.
     */
    fun publishResourceWrappers(
        tenantId: String,
        resourceWrappers: List<PublishResourceWrapper>,
        metadata: Metadata,
        dataTrigger: DataTrigger? = null,
    ): PublishResponse {
        val resourceWrappersByResourceId = resourceWrappers.associateBy { it.id!!.value }
        val resourcesById = resourceWrappers.associate { it.id!!.value to it.resource }
        val response = runBlocking { ehrDataAuthorityClient.addResources(tenantId, resourcesById.values.toList()) }

        val succeeded = response.succeeded
        if (succeeded.isNotEmpty()) {
            // Publish only modified resources to Datalake
            val modifiedSuccessfulResources =
                succeeded.filterNot { it.modificationType == ModificationType.UNMODIFIED }
                    .map { resourceWrappersByResourceId[it.resourceId]!! }
            if (modifiedSuccessfulResources.isNotEmpty()) {
                datalakePublishService.publishFHIRR4(tenantId, modifiedSuccessfulResources.map { it.resource })
            }

            // Publish all resources to Kafka, if a data trigger was provided
            val allSuccessfulResources = succeeded.map { resourceWrappersByResourceId[it.resourceId]!! }
            dataTrigger?.let {
                kafkaPublishService.publishResourceWrappers(
                    tenantId,
                    dataTrigger,
                    allSuccessfulResources,
                    metadata,
                )
            }
        }

        return PublishResponse(
            successfulIds = succeeded.map { it.resourceId },
            failedIds = response.failed.map { it.resourceId },
        )
    }
}
