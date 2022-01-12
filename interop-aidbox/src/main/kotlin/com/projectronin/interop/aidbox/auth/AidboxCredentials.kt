package com.projectronin.interop.aidbox.auth

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * Credentials for accessing Aidbox.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AidboxCredentials(
    val clientId: String,
    val clientSecret: String
) {
    val grantType: String = "client_credentials"
}
