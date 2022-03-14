package com.projectronin.interop.publishers

import com.projectronin.interop.fhir.FHIRResource
import com.projectronin.interop.aidbox.PublishService as AidboxPublishService

/**
 * Service managing the publication of data to one or more downstream repositories.
 */
class PublishService(private val aidboxPublishService: AidboxPublishService) {
    /**
     * Publishes the supplied [resources]. If all resources were successfully published, true will be returned; otherwise, false will be returned.
     */
    fun publishFHIRResources(resources: List<FHIRResource>): Boolean {
        return aidboxPublishService.publish(resources)
    }
}
