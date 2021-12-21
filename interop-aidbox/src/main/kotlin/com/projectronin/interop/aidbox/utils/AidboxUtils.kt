package com.projectronin.interop.aidbox.utils

import com.projectronin.interop.aidbox.client.model.GraphQLError
import com.projectronin.interop.aidbox.client.model.GraphQLResponse
import io.ktor.client.call.receive
import io.ktor.client.features.ResponseException

/**
 * @return The namespace associated to the Ronin tenant identifier.
 */
fun roninTenantNamespace() = "http://projectronin.com/id/tenantId"

/**
 * Creates and returns the appropriate GraphQLResponse for the provided exception.
 * @param exception The exception for which a response should be created
 * @return The [GraphQLResponse] appropriate for the exception.
 */
suspend fun <T> respondToException(exception: Exception): GraphQLResponse<T> {
    val httpResponse = when (exception) {
        // This covers all of the types of responses handled by Ktor.
        is ResponseException -> exception.response
        else -> throw exception
    }

    val graphQLError =
        GraphQLError("Error communicating with Aidbox. Received status code ${httpResponse.status} with message \"${httpResponse.receive<String>()}\"")
    return GraphQLResponse(errors = listOf(graphQLError))
}
