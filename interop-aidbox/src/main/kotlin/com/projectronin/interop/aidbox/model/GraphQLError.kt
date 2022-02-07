package com.projectronin.interop.aidbox.model

data class GraphQLError(
    val message: String,
    val path: List<Any>? = null,
    val extensions: Map<String, Any?>? = null
)
