package com.projectronin.interop.aidbox.model

data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null,
    val extensions: Map<String, Any?>? = null
)
