package com.projectronin.interop.aidbox.client

import com.projectronin.interop.aidbox.auth.AidboxAuthenticationBroker
import com.projectronin.interop.aidbox.model.GraphQLPostRequest
import com.projectronin.interop.aidbox.utils.makeBundleForBatchUpsert
import com.projectronin.interop.common.http.request
import com.projectronin.interop.fhir.r4.resource.Resource
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Client for accessing an Aidbox server via its configured base URL for REST API calls.
 */
@Component
class AidboxClient(
    private val httpClient: HttpClient,
    @Value("\${aidbox.url}")
    private val aidboxURLRest: String,
    private val authenticationBroker: AidboxAuthenticationBroker
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Publishes FHIR resources to Aidbox by POSTing a FHIR transaction bundle. Expects an id value in each resource.
     * For an existing resource id, updates that resource with the new data. For a new id, adds the resource to Aidbox.
     * Order of resources in the bundle is not important to resolve references within the bundle. The only requirement
     * on references is that all the referenced resources are either in the bundle or already in Aidbox.
     * The transaction bundle is all-or-nothing: Every resource in the bundle must succeed to return a 200 response.
     * @param resourceCollection List of FHIR resources to publish. May be a mixed List with different resourceTypes.
     * @return [HttpResponse] from the Aidbox FHIR transaction bundle REST API.
     */
    suspend fun batchUpsert(resourceCollection: List<Resource<*>>): HttpResponse {
        val arrayLength = resourceCollection.size
        val showArray = when (arrayLength) {
            1 -> "resource"
            else -> "resources"
        }
        logger.debug { "Aidbox batch upsert of $arrayLength $showArray" }
        val bundle = makeBundleForBatchUpsert(aidboxURLRest, resourceCollection)
        val authentication = authenticationBroker.getAuthentication()
        val response: HttpResponse = runBlocking {
            httpClient.request("Aidbox", "$aidboxURLRest/fhir") { url ->
                post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "${authentication.tokenType} ${authentication.accessToken}")
                        append("aidbox-validation-skip", "reference")
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(bundle)
                }
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
        val response: HttpResponse = httpClient.request("Aidbox", "$aidboxURLRest/\$graphql") { url ->
            post(url) {
                headers {
                    append(HttpHeaders.Authorization, "${authentication.tokenType} ${authentication.accessToken}")
                }
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(GraphQLPostRequest(query = query, variables = parameters.toSortedMap()))
            }
        }

        logger.debug { "Aidbox query returned ${response.status}" }

        return response
    }

    /**
     * Fetches a full FHIR resource from Aidbox based on the Fhir ID.
     * @param resourceType [String] the type of FHIR resource, i.e. "Patient" (case sensitive)
     * @param resourceFHIRID [String] the FHIR ID of the resource ("id" json element)
     * @return [HttpResponse] containing the raw data from the server. Use HttpResponse.recieve<T>() to deserialize.
     */
    suspend fun getResource(resourceType: String, resourceFHIRID: String): HttpResponse {
        val authentication = authenticationBroker.getAuthentication()
        val response: HttpResponse =
            httpClient.request("Aidbox", "$aidboxURLRest/fhir/$resourceType/$resourceFHIRID") { url ->
                get(url) {
                    headers {
                        append(HttpHeaders.Authorization, "${authentication.tokenType} ${authentication.accessToken}")
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                }
            }

        return response
    }
}
