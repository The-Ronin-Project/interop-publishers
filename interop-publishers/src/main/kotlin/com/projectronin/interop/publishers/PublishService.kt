package com.projectronin.interop.publishers

import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.publishers.exception.AidboxPublishException
import com.projectronin.interop.aidbox.PublishService as AidboxPublishService

/**
 * Service managing the publication of data to one or more downstream repositories.
 */
class PublishService(
    private val aidboxPublishService: AidboxPublishService,
    private val datalakePublishService: DatalakePublishService
) {
    /**
     * Publishes the supplied [resources]. If all resources were successfully published,
     * true will be returned.  Otherwise, [AidboxPublishException] will be thrown.
     */
    fun publishFHIRResources(tenantId: String, resources: List<Resource<*>>): Boolean {
        if (!aidboxPublishService.publish(resources)) {
            throw AidboxPublishException("Could not publish resources to Aidbox for tenant $tenantId")
        }
        datalakePublishService.publishFHIRR4(tenantId, resources)
        return true
    }
}
