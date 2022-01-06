package com.projectronin.interop.aidbox.client

import com.projectronin.interop.aidbox.utils.respondToException
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.RedirectResponseException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

/**
 * Client for accessing Aidbox.
 */
class AidboxClient(
    private val httpClient: HttpClient,
    private val urlRest: String
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Publishes resources to Aidbox via its REST API at its configured base URL.
     * Sends a PUT / call with the raw JSON String for a collection of raw JSON resources (not a FHIR bundle).
     * The resources in this collection may be any mix of FHIR resource types. Note that for efficiency,
     * Aidbox does not validate the resources submitted using PUT / (as is used here) or $import or $load.
     * PUT / does not require an id to be on any resource, but in practice, we expect to provide id values in this data.
     * For an existing Aidbox id, PUT / updates that resource with the new data. For a new id, it adds the resource.
     * @param rawJsonCollection Stringified raw JSON array of strings that each represent a FHIR resource to publish.
     * @return Boolean, true only for a 200 response. We do not examine the payload in the 200 response from this call.
     * @throws RedirectResponseException for a 3xx response.
     * @throws ClientRequestException for a 4xx response.
     * @throws ServerResponseException for a 5xx response.
     */
    suspend fun publish(rawJsonCollection: String, authString: String): Boolean {
        val arrayLength = "\"resourceType\"".toRegex().findAll(rawJsonCollection).count()
        val showArray = when (arrayLength) {
            1 -> "resource"
            else -> "resources"
        }
        logger.debug { "Aidbox publish of $arrayLength $showArray" }

        val response: HttpResponse = runBlocking {
            try {
                httpClient.put("$urlRest/") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $authString")
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    body = rawJsonCollection
                }
            } catch (e: Exception) {
                respondToException<HttpResponse>(e)
            }
        }

        val statusText = response.status.toString()
        val message = "Aidbox publish returned $statusText"
        logger.debug { message }

        when (statusText.substring(0, 1)) {
            "3" -> throw RedirectResponseException(response, message)
            "4" -> throw ClientRequestException(response, message)
            "5" -> throw ServerResponseException(response, message)
        }

        return (statusText.substring(0, 3) == "200")
    }
}
