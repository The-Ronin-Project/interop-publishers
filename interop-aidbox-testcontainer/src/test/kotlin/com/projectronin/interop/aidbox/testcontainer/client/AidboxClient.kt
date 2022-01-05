package com.projectronin.interop.aidbox.testcontainer.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.projectronin.interop.aidbox.testcontainer.client.graphql.GraphQLPostRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class AidboxClient(private val aidboxGraphQLUrl: String) {
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer() {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
    }

    suspend fun query(query: String, authString: String, parameters: Map<String, String> = mapOf()): HttpResponse =
        httpClient.post(aidboxGraphQLUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authString")
            }
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)

            body = GraphQLPostRequest(query = query, variables = parameters.toSortedMap())
        }
}
