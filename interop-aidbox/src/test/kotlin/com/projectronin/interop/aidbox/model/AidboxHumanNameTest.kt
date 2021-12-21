package com.projectronin.interop.aidbox.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AidboxHumanNameTest {
    @Test
    fun `check defaults`() {
        val address = AidboxHumanName()
        assertEquals(null, address.use)
        assertEquals(null, address.family)
        assertEquals(null, address.given)
    }

    @Test
    fun `check getters`() {
        val name = AidboxHumanName(use = "official", family = "Smith", given = listOf("Josh"))
        assertEquals("official", name.use)
        assertEquals("Smith", name.family)
        assertEquals(listOf("Josh"), name.given)
    }
}
