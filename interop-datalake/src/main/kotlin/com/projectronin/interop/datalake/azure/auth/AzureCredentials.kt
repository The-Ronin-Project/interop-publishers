package com.projectronin.interop.datalake.azure.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class AzureCredentials(
    @Value("\${datalake.azure.client.id}")
    val clientId: String,
    @Value("\${datalake.azure.client.secret}")
    val clientSecret: String
) {
    // Override toString() to prevent accidentally leaking the clientSecret
    override fun toString(): String = this::class.simpleName!!
}
