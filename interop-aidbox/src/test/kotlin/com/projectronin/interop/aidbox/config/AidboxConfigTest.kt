package com.projectronin.interop.aidbox.config

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AidboxConfigTest {
    @Test
    fun `ensure config can be created and http client can be retrieved`() {
        val config = AidboxConfig()
        val httpClient = config.getHttpClient()
        assertNotNull(httpClient)
    }
}
