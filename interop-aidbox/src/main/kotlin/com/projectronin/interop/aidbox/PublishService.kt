package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.utils.respondToException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking

/**
 * Service allowing access to push data updates to Aidbox.
 */
class PublishService(private val aidboxClient: AidboxClient) {

    /**
     * Publishes resources to Aidbox via its REST API. Expects an id to be supplied in each resource.
     * For an existing resource id, publish updates that resource with the new data. For a new id, it adds the resource.
     * @param rawJsonCollection Stringified raw JSON array of strings that each represent a FHIR resource to publish.
     * @return Boolean true only for a 200 OK response from Aidbox; otherwise, even if 1xx or 2xx, false.
     */
    fun publish(rawJsonCollection: String): Boolean {
        val response: HttpResponse = runBlocking {
            try {
                aidboxClient.batchUpsert(rawJsonCollection)
            } catch (e: Exception) {
                respondToException<HttpResponse>(e)
            }
        }
        return (response.status == HttpStatusCode.OK)
    }
}
