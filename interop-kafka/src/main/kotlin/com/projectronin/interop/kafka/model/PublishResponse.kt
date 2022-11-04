package com.projectronin.interop.kafka.model

data class PublishResponse<T>(
    val successful: List<T> = listOf(),
    val failures: List<Failure<T>> = listOf()
)

data class Failure<T>(
    val data: T,
    val error: Throwable
)
