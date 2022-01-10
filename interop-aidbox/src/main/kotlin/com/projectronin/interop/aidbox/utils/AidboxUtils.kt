package com.projectronin.interop.aidbox.utils

import io.ktor.client.features.ResponseException
import io.ktor.client.statement.HttpResponse
import mu.KotlinLogging

/**
 * Creates and returns the appropriate HTTPResponse for the provided exception.
 * @param exception The exception for which a response should be created
 * @return The [HTTPResponse] appropriate for the exception.
 */
fun <T> respondToException(exception: Exception): HttpResponse {
    val httpResponse = when (exception) {
        // This covers all of the types of responses handled by Ktor.
        is ResponseException -> exception.response
        else -> throw exception
    }
    val logger = KotlinLogging.logger { }
    logger.debug { "Error communicating with Aidbox: ${httpResponse.status}" }
    return exception.response
}
