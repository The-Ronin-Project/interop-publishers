package com.projectronin.interop.aidbox.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AidboxAddressTest {
    @Test
    fun `check defaults`() {
        val address = AidboxAddress()
        assertEquals(null, address.use)
        assertEquals(null, address.line)
        assertEquals(null, address.city)
        assertEquals(null, address.state)
        assertEquals(null, address.postalCode)
    }

    @Test
    fun `check getters`() {
        val address = AidboxAddress(
            use = "home",
            line = listOf("1234 Main St"),
            city = "Anywhere",
            state = "FL",
            postalCode = "37890"
        )
        assertEquals("home", address.use)
        assertEquals(listOf("1234 Main St"), address.line)
        assertEquals("Anywhere", address.city)
        assertEquals("FL", address.state)
        assertEquals("37890", address.postalCode)
    }
}
