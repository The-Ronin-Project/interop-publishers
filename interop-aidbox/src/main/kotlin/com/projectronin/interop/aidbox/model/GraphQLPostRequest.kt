package com.projectronin.interop.aidbox.model

data class GraphQLPostRequest(
    val query: String,
    val operationName: String? = null,
    val variables: Map<String, Any?>? = null
)
