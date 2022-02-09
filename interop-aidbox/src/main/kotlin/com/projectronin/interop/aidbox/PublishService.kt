package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.utils.respondToException
import com.projectronin.interop.fhir.r4.ronin.resource.RoninDomainResource
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * Service allowing access to push data updates to Aidbox.
 */
class PublishService(private val aidboxClient: AidboxClient) {
    private val logger = KotlinLogging.logger { }
    /**
     * Publishes resources to Aidbox via its REST API for Batch Upsert. Expects an id value in each resource.
     * For an existing resource id, publish updates that resource with the new data. For a new id, it adds the resource.
     * @param resourceCollection List of FHIR resources to publish. May be a mixed List with different resourceTypes.
     * @return Boolean true only for a 200 OK response from Aidbox; otherwise, even if 1xx or 2xx, false.
     */
    fun publish(resourceCollection: List<RoninDomainResource>): Boolean {
        logger.info { "Publishing to Aidbox" }
        val response: HttpResponse = runBlocking {
            try {
                aidboxClient.batchUpsert(resourceCollection)
            } catch (e: Exception) {
                logger.error(e) { "Failed to publish to Aidbox" }
                respondToException<HttpResponse>(e)
            }
        }
        return (response.status == HttpStatusCode.OK)
    }
}
