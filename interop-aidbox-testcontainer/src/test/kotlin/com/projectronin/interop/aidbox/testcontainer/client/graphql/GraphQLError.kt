package com.projectronin.interop.aidbox.testcontainer.client.graphql

/**
 * Describes a GraphQL error as defined by the [GraphQL Specification](https://spec.graphql.org/June2018/#sec-Errors).
 */
data class GraphQLError(
    val message: String,
    val locations: List<GraphQLErrorLocation>? = null,
    val path: List<Any>? = null,
    val extensions: Map<String, Any?>? = null,
)
