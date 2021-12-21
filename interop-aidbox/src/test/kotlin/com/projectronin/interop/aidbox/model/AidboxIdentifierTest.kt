package com.projectronin.interop.aidbox.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AidboxIdentifierTest {
    @Test
    fun `check defaults`() {
        val address = AidboxIdentifier()
        assertEquals(null, address.system)
        assertEquals(null, address.value)
    }

    @Test
    fun `check getters`() {
        val identifier = AidboxIdentifier(system = "http://hl7.org/fhir/sid/us-ssn", value = "987-65-4321")
        assertEquals("http://hl7.org/fhir/sid/us-ssn", identifier.system)
        assertEquals("987-65-4321", identifier.value)
    }
}
