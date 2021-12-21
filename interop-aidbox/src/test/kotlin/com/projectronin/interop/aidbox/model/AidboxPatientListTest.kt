package com.projectronin.interop.aidbox.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AidboxPatientListTest {
    @Test
    fun `check defaults`() {
        val address = AidboxPatientList()
        assertEquals(listOf<AidboxPatient>(), address.patientList)
    }

    @Test
    fun `check getters`() {
        val patient = AidboxPatient(id = "Patient-UUID-1")
        val patientList = AidboxPatientList(listOf(patient))
        assertEquals(listOf(patient), patientList.patientList)
    }
}
