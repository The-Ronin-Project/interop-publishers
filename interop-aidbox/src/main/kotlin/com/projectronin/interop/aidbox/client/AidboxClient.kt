package com.projectronin.interop.aidbox.client

import com.projectronin.interop.aidbox.auth.AuthenticationBroker
import com.projectronin.interop.aidbox.model.GraphQLPostRequest
import com.projectronin.interop.fhir.r4.ronin.resource.RoninDomainResource
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.RedirectResponseException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Client for accessing an Aidbox server via its configured base URL for REST API calls.
 */
@Component
class AidboxClient(
    @Qualifier("AidboxHTTPClient")
    private val httpClient: HttpClient,
    @Value("\${aidbox.url}")
    private val aidboxURLRest: String,
    @Qualifier("AidboxAuthBroker")
    private val authenticationBroker: AuthenticationBroker
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Provides access to the Aidbox Batch Upsert feature, see https://docs.aidbox.app/api-1/batch-upsert.
     * Publishes resources to Aidbox via its REST API for Batch Upsert. Expects an id value in each resource.
     * For an existing resource id, publish updates that resource with the new data. For a new id, it adds the resource.
     * For efficiency, Aidbox does not validate the resources submitted using PUT / (Batch Upsert) or $import or $load.
     * PUT / does not require an id to be on any resource, but we expect to provide id values when we put this data.
     * @param resourceCollection List of FHIR resources to publish. May be a mixed List with different resourceTypes.
     * @return [HttpResponse] from the Batch Upsert API.
     * @throws [RedirectResponseException] for a 3xx response.
     * @throws [ClientRequestException] for a 4xx response.
     * @throws [ServerResponseException] for a 5xx response.
     */
    suspend fun batchUpsert(resourceCollection: List<RoninDomainResource>): HttpResponse {
        val arrayLength = resourceCollection.size
        val showArray = when (arrayLength) {
            1 -> "resource"
            else -> "resources"
        }
        logger.debug { "Aidbox batch upsert of $arrayLength $showArray" }
        val authentication = authenticationBroker.getAuthentication()
        val response: HttpResponse = runBlocking {
            try {
                httpClient.put("$aidboxURLRest/") {
                    headers {
                        append(HttpHeaders.Authorization, "${authentication.tokenType} ${authentication.accessToken}")
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    body = resourceCollection
                }
            } catch (e: Exception) {
                logger.error(e) { "Exception during Aidbox batch upsert" }
                throw e
            }
        }

        return response
    }

    /**
     * Provides access to the Aidbox GraphQL server feature.
     * Executes any graphQL query against the correct endpoint and returns the HttpResponse.
     * @param query [String] containing the properly formatted graphQL-style query
     * @param parameters [Map] holds variables to be used in the query
     * @return [HttpResponse] containing the raw data from the server. Use HttpResponse.recieve<T>() to deserialize.
     */
    suspend fun queryGraphQL(query: String, parameters: Map<String, String>): HttpResponse {

        logger.debug { "Processing Aidbox query: $query" }
        val authentication = authenticationBroker.getAuthentication()
        val response: HttpResponse = httpClient.post("$aidboxURLRest/\$graphql") {
            headers {
                append(HttpHeaders.Authorization, "${authentication.tokenType} ${authentication.accessToken}")
            }
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)

            body = GraphQLPostRequest(query = query, variables = parameters.toSortedMap())
        }

        logger.debug { "Aidbox query returned ${response.status}" }

        return response
    }
}
