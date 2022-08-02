package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.utils.respondToException
import com.projectronin.interop.fhir.r4.resource.Resource
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Service allowing access to push data updates to the Ronin clinical data store.
 */
@Service
class PublishService(private val aidboxClient: AidboxClient) {
    private val logger = KotlinLogging.logger { }

    /**
     * Publishes FHIR resources to the Ronin clinical data store via HTTP. Expects an id value in each resource.
     * For an existing resource id, publish updates that resource with the new data. For a new id, it adds the resource.
     * Expects that the caller will not input an empty List.
     * @param resourceCollection List of FHIR resources to publish. May be a mixed List with different resourceTypes.
     * @return true for success: an HTTP 2xx response, or publish was skipped for an empty list; otherwise false.
     */
    fun publish(resourceCollection: List<Resource<*>>): Boolean {
        logger.info { "Publishing Ronin clinical data" }
        if (resourceCollection.isEmpty()) {
            return true
        }
        val response: HttpResponse = runBlocking {
            try {
                aidboxClient.batchUpsert(resourceCollection)
            } catch (e: Exception) {
                logger.error(e) { "Failed to publish Ronin clinical data" }
                respondToException<HttpResponse>(e)
            }
        }
        return (response.status.isSuccess())
    }
}
