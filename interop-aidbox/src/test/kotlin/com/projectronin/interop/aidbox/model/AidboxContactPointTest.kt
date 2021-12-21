package com.projectronin.interop.aidbox.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AidboxContactPointTest {
    @Test
    fun `check defaults`() {
        val address = AidboxContactPoint()
        assertEquals(null, address.system)
        assertEquals(null, address.use)
        assertEquals(null, address.value)
    }

    @Test
    fun `check getters`() {
        val contact = AidboxContactPoint(system = "phone", use = "mobile", value = "123-456-7890")
        assertEquals("phone", contact.system)
        assertEquals("mobile", contact.use)
        assertEquals("123-456-7890", contact.value)
    }
}
