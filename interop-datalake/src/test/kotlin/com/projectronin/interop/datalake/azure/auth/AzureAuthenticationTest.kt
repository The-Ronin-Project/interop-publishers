package com.projectronin.interop.datalake.azure.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class AzureAuthenticationTest {

    @Test
    fun `getters and setters`() {
        val instant = OffsetDateTime.now().toInstant()
        val auth = AzureAuthentication("token", instant)
        assertEquals("token", auth.accessToken)
        assertEquals(instant, auth.expiresAt)
        assertEquals("SAS", auth.tokenType)
        assertNull(auth.scope)
        assertNull(auth.refreshToken)
    }

    @Test
    fun `toString overridden`() {
        val instant = OffsetDateTime.now().toInstant()
        val auth = AzureAuthentication("token", instant)
        assertEquals("AzureAuthentication", auth.toString())
    }
}
