package com.projectronin.interop.aidbox.auth

import com.projectronin.interop.common.auth.Authentication
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Brokers [Authentication] allowing re-use of existing credentials as long as they have not expired.
 */
@Component
class AuthenticationBroker(private val authenticationService: AuthenticationService) {
    private val logger = KotlinLogging.logger { }
    private val expirationBuffer: Long = 60 // seconds
    private var cachedAuthentication: Authentication? = null

    /**
     * Retrieves the current [Authentication] to use.
     */
    fun getAuthentication(): Authentication {
        val isCacheValid =
            cachedAuthentication?.expiresAt?.isAfter(Instant.now().plusSeconds(expirationBuffer)) ?: false
        if (isCacheValid) {
            logger.debug { "Returning cached authentication for Aidbox" }
            return cachedAuthentication!!
        }

        logger.debug { "Requesting fresh authentication for Aidbox" }
        val authentication = authenticationService.getAuthentication()
        authentication.expiresAt?.let {
            logger.debug { "Retrieved authentication Aidbox has expiration (${authentication.expiresAt}), so it will be cached" }
            cachedAuthentication = authentication
        }
        return authentication
    }
}
