package com.projectronin.interop.aidbox.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AidboxPatientTest {
    @Test
    fun `check defaults`() {
        val address = AidboxPatient()
        assertEquals(null, address.id)
        assertEquals(null, address.identifier)
        assertEquals(null, address.name)
        assertEquals(null, address.birthDate)
        assertEquals(null, address.gender)
        assertEquals(null, address.telecom)
        assertEquals(null, address.address)
    }

    @Test
    fun `check getters`() {
        val identifier = AidboxIdentifier(system = "http://hl7.org/fhir/sid/us-ssn", value = "987-65-4321")
        val name = AidboxHumanName(family = "Smith", given = listOf("Josh"))
        val contact = AidboxContactPoint(value = "123-456-7890")
        val address = AidboxAddress(state = "FL")
        val patient = AidboxPatient(
            id = "Patient-UUID-1",
            identifier = listOf(identifier),
            name = listOf(name),
            birthDate = "1984-08-31",
            gender = "male",
            telecom = listOf(contact),
            address = listOf(address)
        )
        assertEquals("Patient-UUID-1", patient.id)
        assertEquals(listOf(identifier), patient.identifier)
        assertEquals(listOf(name), patient.name)
        assertEquals("1984-08-31", patient.birthDate)
        assertEquals("male", patient.gender)
        assertEquals(listOf(contact), patient.telecom)
        assertEquals(listOf(address), patient.address)
    }
}
