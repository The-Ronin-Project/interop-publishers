package com.projectronin.interop.aidbox.client.model

/**
 * Describes a GraphQL post body as seen in [GraphQL Documentation](https://graphql.org/learn/serving-over-http/#post-request)
 */
data class GraphQLPostRequest(
    val query: String,
    val operationName: String? = null,
    val variables: Map<String, Any?>? = null
)
