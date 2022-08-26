package com.projectronin.interop.datalake.azure.auth

import com.projectronin.interop.common.auth.Authentication
import java.time.Instant

/**
 * Datalake implementation of [Authentication]
 */
data class AzureAuthentication(
    override val accessToken: String,
    override val expiresAt: Instant,
    override val refreshToken: String? = null,
    override val scope: String? = null
) : Authentication {
    override val tokenType = "SAS"

    // Override toString() to prevent accidentally leaking the accessToken
    override fun toString(): String = this::class.simpleName!!
}
