package com.projectronin.interop.publishers

import com.projectronin.event.interop.internal.v1.Metadata
import com.projectronin.interop.aidbox.AidboxPublishService
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.kafka.KafkaPublishService
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.publishers.exception.AidboxPublishException
import org.springframework.stereotype.Service

/**
 * Service managing the publication of data to one or more downstream repositories.
 */
@Service
class PublishService(
    private val aidboxPublishService: AidboxPublishService,
    private val datalakePublishService: DatalakePublishService,
    private val kafkaPublishService: KafkaPublishService
) {
    /**
     * Publishes the supplied [resources]. If all resources were successfully published,
     * true will be returned.  Otherwise, [AidboxPublishException] will be thrown.
     */
    fun publishFHIRResources(
        tenantId: String,
        resources: List<Resource<*>>,
        metadata: Metadata,
        dataTrigger: DataTrigger? = null
    ): Boolean {
        if (!aidboxPublishService.publish(resources)) {
            throw AidboxPublishException("Could not publish resources to Aidbox for tenant $tenantId")
        }
        datalakePublishService.publishFHIRR4(tenantId, resources)
        dataTrigger?.let { kafkaPublishService.publishResources(tenantId, dataTrigger, resources, metadata) }
        return true
    }
}
