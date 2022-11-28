package com.projectronin.interop.aidbox.utils

import com.projectronin.interop.aidbox.exception.InvalidTenantAccessException
import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.common.http.exceptions.HttpException
import com.projectronin.interop.fhir.r4.datatype.BundleEntry
import com.projectronin.interop.fhir.r4.datatype.BundleRequest
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.BundleType
import com.projectronin.interop.fhir.r4.valueset.HttpVerb

/**
 * Creates and returns the appropriate GraphQLReponse for the provided exception.
 * @param exception The exception for which a response should be created
 * @return The [GraphQLResponse] appropriate for the exception.
 */
fun <T> respondToGraphQLException(exception: Exception): GraphQLResponse<T> {
    if (exception !is HttpException) throw exception

    val graphQLError = GraphQLError(exception.message ?: "")
    return GraphQLResponse(errors = listOf(graphQLError))
}

/**
 * Creates and returns a FHIR transaction bundle for posting to Aidbox using the AidboxClient batchUpsert() method.
 * @param aidboxURLRest The root URL for Aidbox FHIR REST calls.
 * @param resources The FHIR resources for the bundle.
 */
fun makeBundleForBatchUpsert(aidboxURLRest: String, resources: List<Resource<*>>): Bundle {
    return Bundle(
        id = null,
        type = Code(BundleType.TRANSACTION.code),
        entry = resources.map { makeBundleEntry(aidboxURLRest, HttpVerb.PUT, it) }
    )
}

/**
 * Creates and returns one [BundleEntry] within a FHIR transaction bundle for posting to Aidbox.
 * @param aidboxURLRest The root URL for Aidbox FHIR REST calls.
 * @param method The HTTP verb for the entry, i.e. PUT, DELETE, etc.
 * @param resource The FHIR resource for the entry. The id and resourceType values must be present.
 */
fun makeBundleEntry(aidboxURLRest: String, method: HttpVerb, resource: Resource<*>): BundleEntry {
    val fullReference = "/${resource.resourceType}/${resource.id?.value}"
    return BundleEntry(
        fullUrl = Uri("$aidboxURLRest$fullReference"),
        request = BundleRequest(method = Code(method.code), url = Uri(fullReference)),
        resource = resource
    )
}

/**
 * Validates a Ronin Tenant identifier matches the provided [tenantMnemonic] on the [identifiers],
 * throws [InvalidTenantAccessException] with the supplied [errorMessage] if failed.
 */
fun validateTenantIdentifier(tenantMnemonic: String, identifiers: List<Identifier>, errorMessage: String) {
    if (identifiers.none { it.value?.value == tenantMnemonic && it.system?.value == "http://projectronin.com/id/tenantId" }) {
        throw InvalidTenantAccessException(errorMessage)
    }
}
