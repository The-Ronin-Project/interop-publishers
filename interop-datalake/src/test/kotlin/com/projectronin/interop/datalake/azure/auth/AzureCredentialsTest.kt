package com.projectronin.interop.datalake.azure.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AzureCredentialsTest {
    @Test
    fun `ensures toString is overwritten`() {
        val credentials = AzureCredentials(
            clientId = "id",
            clientSecret = "secret"
        )
        assertEquals("AzureCredentials", credentials.toString())
    }

    @Test
    fun `check getters and setters`() {
        val credentials = AzureCredentials(
            clientId = "id",
            clientSecret = "secret"
        )
        assertEquals("id", credentials.clientId)
        assertEquals("secret", credentials.clientSecret)
    }
}
