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
 * Client for accessing an Aidbox server via its configured base URL for REST API calls.
 */
class AidboxClient(
    private val httpClient: HttpClient,
    private val aidboxURLRest: String,
    private val aidboxAuthString: String
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Provides access to the Aidbox Batch Upsert feature, see https://docs.aidbox.app/api-1/batch-upsert.
     * Sends a raw JSON String that represents an array of JSON resources (not a FHIR bundle).
     * The resources in this array may be any mix of FHIR resource types. Note that for efficiency,
     * Aidbox does not validate the resources submitted using PUT / (Batch Upsert) or $import or $load.
     * PUT / does not require an id to be on any resource, but we expect to provide id values in this data.
     * For an existing Aidbox id, PUT / updates that resource with the new data. For a new id, it adds the resource.
     * @param rawJsonCollection Stringified raw JSON array of strings that each represent a FHIR resource to publish.
     * @return [HttpResponse] from the "Batch Upsert" API.
     * @throws [RedirectResponseException] for a 3xx response.
     * @throws [ClientRequestException] for a 4xx response.
     * @throws ServerResponseException for a 5xx response.
     */
    suspend fun batchUpsert(rawJsonCollection: String): HttpResponse {
        val arrayLength = "\"resourceType\"".toRegex().findAll(rawJsonCollection).count()
        val showArray = when (arrayLength) {
            1 -> "resource"
            else -> "resources"
        }
        logger.debug { "Aidbox Batch Upsert of $arrayLength $showArray" }

        val response: HttpResponse = runBlocking {
            try {
                httpClient.put("$aidboxURLRest/") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $aidboxAuthString")
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
        val message = "Aidbox Batch Upsert responded $statusText"
        logger.debug { message }

        when (statusText.substring(0, 1)) {
            "3" -> throw RedirectResponseException(response, message)
            "4" -> throw ClientRequestException(response, message)
            "5" -> throw ServerResponseException(response, message)
        }

        return response
    }
}
