package com.projectronin.interop.aidbox.client.model

/**
 * Describes a GraphQL response as defined by the [GraphQL Specification](https://spec.graphql.org/June2018/#sec-Response-Format).
 */
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null,
    val extensions: Map<String, Any?>? = null
)
