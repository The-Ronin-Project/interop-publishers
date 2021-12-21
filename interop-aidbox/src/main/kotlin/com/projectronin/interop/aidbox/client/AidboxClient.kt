package com.projectronin.interop.aidbox.client

import com.projectronin.interop.aidbox.client.model.GraphQLPostRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Client for accessing Aidbox.
 */
@Component
class AidboxClient(
    @Qualifier("aidbox")
    private val httpClient: HttpClient,
    @Value("\${aidbox.graphql.url}") private val aidboxGraphQLUrl: String
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Submits the given query to Aidbox and returns the resulting response.
     * @param query The parameterized query that should be run through Aidbox
     * @param authString The authorization string that should be provided to Aidbox
     * @param parameters The optional Map of parameters. The key should match the parameter name provided in the query.
     * @return The HTTP Response. Consumers can then use the [receive] function to extract the parameterized
     * [com.projectronin.interop.aidbox.model.GraphQLResponse] object. This is required due to issues with Jacoco and inline functions.
     * @throws RedirectResponseException for a 3xx response.
     * @throws ClientRequestException for a 4xx response.
     * @throws ServerResponseException for a 5xx response.
     */
    suspend fun query(query: String, authString: String, parameters: Map<String, String> = mapOf()): HttpResponse {
        // We're intentionally not logging the parameters here since they could have PHI.
        logger.debug { "Processing Aidbox query: $query" }

        val response: HttpResponse = httpClient.post(aidboxGraphQLUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authString")
            }
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)

            body = GraphQLPostRequest(query = query, variables = parameters.toSortedMap())
        }

        logger.debug { "Aidbox query returned ${response.status}" }

        return response
    }
}
