package com.projectronin.interop.aidbox.testcontainer.client.graphql

/**
 * Describes a GraphQL error location as defined by the [GraphQL Specification](https://spec.graphql.org/June2018/#sec-Errors).
 */
data class GraphQLErrorLocation(
    val line: Int,
    val column: Int,
)
