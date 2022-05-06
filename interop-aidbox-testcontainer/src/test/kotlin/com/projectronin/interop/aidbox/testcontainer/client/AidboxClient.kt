package com.projectronin.interop.aidbox.testcontainer.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.projectronin.interop.aidbox.testcontainer.client.graphql.GraphQLPostRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson

class AidboxClient(private val aidboxGraphQLUrl: String) {
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            }
        }
        expectSuccess = true
    }

    suspend fun query(query: String, authString: String, parameters: Map<String, String> = mapOf()): HttpResponse =
        httpClient.post(aidboxGraphQLUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $authString")
            }
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(GraphQLPostRequest(query = query, variables = parameters.toSortedMap()))
        }
}
