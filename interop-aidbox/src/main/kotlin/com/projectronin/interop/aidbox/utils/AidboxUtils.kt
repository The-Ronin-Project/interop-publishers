package com.projectronin.interop.aidbox.utils

import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.fhir.datatype.BundleEntry
import com.projectronin.interop.aidbox.model.fhir.resource.Bundle
import com.projectronin.interop.fhir.FHIRResource
import com.projectronin.interop.fhir.r4.datatype.BundleRequest
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.valueset.BundleType
import com.projectronin.interop.fhir.r4.valueset.HttpVerb
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
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
        GraphQLError("Error communicating with Aidbox. Received status code ${httpResponse.status} with message \"${httpResponse.bodyAsText()}\"")
    return GraphQLResponse(errors = listOf(graphQLError))
}

/**
 * Creates and returns a FHIR transaction bundle for posting to Aidbox using the AidboxClient batchUpsert() method.
 * @param aidboxURLRest The root URL for Aidbox FHIR REST calls.
 * @param resources The FHIR resources for the bundle.
 */
fun makeBundleForBatchUpsert(aidboxURLRest: String, resources: List<FHIRResource>): Bundle {
    return Bundle(
        id = null,
        type = BundleType.TRANSACTION,
        entry = resources.map { makeBundleEntry(aidboxURLRest, HttpVerb.PUT, it) }
    )
}

/**
 * Creates and returns one [BundleEntry] within a FHIR transaction bundle for posting to Aidbox.
 * @param aidboxURLRest The root URL for Aidbox FHIR REST calls.
 * @param method The HTTP verb for the entry, i.e. PUT, DELETE, etc.
 * @param resource The FHIR resource for the entry. The id and resourceType values must be present.
 */
fun makeBundleEntry(aidboxURLRest: String, method: HttpVerb, resource: FHIRResource): BundleEntry {
    val fullReference = "/${resource.resourceType}/${resource.id?.value}"
    return BundleEntry(
        fullUrl = Uri("$aidboxURLRest$fullReference"),
        request = BundleRequest(method = method, url = Uri(fullReference)),
        resource = resource
    )
}
