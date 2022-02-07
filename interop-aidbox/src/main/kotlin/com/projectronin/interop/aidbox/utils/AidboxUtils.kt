package com.projectronin.interop.aidbox.utils

import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import io.ktor.client.call.receive
import io.ktor.client.features.ResponseException
import io.ktor.client.statement.HttpResponse
import mu.KotlinLogging

/**
 * Creates and returns the appropriate HTTPResponse for the provided exception.
 * @param exception The exception for which a response should be created
 * @return The [HttpResponse] appropriate for the exception.
 */
fun <T> respondToException(exception: Exception): HttpResponse {
    val httpResponse = when (exception) {
        // This covers all of the types of responses handled by Ktor.
        is ResponseException -> exception.response
        else -> throw exception
    }
    KotlinLogging.logger {}.debug { "Error communicating with Aidbox: ${httpResponse.status}" }
    return exception.response
}

/**
 * Creates and returns the appropriate GraphQLReponse for the provided exception.
 * @param exception The exception for which a response should be created
 * @return The [GraphQLResponse] appropriate for the exception.
 */
suspend fun <T> respondToGraphQLException(exception: Exception): GraphQLResponse<T> {
    val httpResponse = when (exception) {
        // This covers all of the types of responses handled by Ktor.
        is ResponseException -> exception.response
        else -> throw exception
    }

    val graphQLError =
        GraphQLError("Error communicating with Aidbox. Received status code ${httpResponse.status} with message \"${httpResponse.receive<String>()}\"")
    return GraphQLResponse(errors = listOf(graphQLError))
}
