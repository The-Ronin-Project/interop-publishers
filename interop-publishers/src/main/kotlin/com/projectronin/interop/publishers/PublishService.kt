package com.projectronin.interop.publishers

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.publishers.exception.AidboxPublishException
import com.projectronin.interop.publishers.exception.DatalakePublishException
import com.projectronin.interop.aidbox.PublishService as AidboxPublishService
import com.projectronin.interop.datalake.PublishService as DatalakePublishService

/**
 * Service managing the publication of data to one or more downstream repositories.
 */
class PublishService(
    private val aidboxPublishService: AidboxPublishService,
    private val datalakePublishService: DatalakePublishService
) {
    /**
     * Publishes the supplied [resources]. If all resources were successfully published to both Aidbox and the datalake,
     * true will be returned.  Otherwise, an exception will be thrown, either [AidboxPublishException] or
     * [DatalakePublishException].
     */
    fun publishFHIRResources(tenantId: String, resources: List<Resource<*>>): Boolean {
        if (!aidboxPublishService.publish(resources)) {
            throw AidboxPublishException("Could not publish resources to Aidbox for tenant $tenantId")
        }

        // Datalake publish returns true or throws an exception
        try {
            datalakePublishService.publishFHIRR4(tenantId, resources)
        } catch (e: Exception) {
            throw DatalakePublishException("Could not publish resources to datalake for tenant $tenantId", e)
        }

        return true
    }
}
