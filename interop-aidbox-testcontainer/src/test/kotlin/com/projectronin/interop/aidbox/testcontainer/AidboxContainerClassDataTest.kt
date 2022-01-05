package com.projectronin.interop.aidbox.testcontainer

import com.projectronin.interop.aidbox.PatientService
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.config.AidboxConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JUnit tests focused on ensuring flows related to class-level data work appropriately.
 */
@AidboxData("aidbox/patients.yaml")
class AidboxContainerClassDataTest : BaseAidboxTest() {
    @Test
    fun `class level data with patient found`() {
        val response = patientService().findPatient("mdaoc", "1984-08-31", "Joshua", "Smith", aidbox.accessToken())

        val errors = response.errors
        assertTrue(errors == null || errors.isEmpty())
        val patients = response.data?.patientList ?: listOf()
        assertEquals(1, patients.size)
        assertEquals("1984-08-31", patients[0].birthDate)
    }

    @Test
    fun `class level data with patient not found`() {
        val response = patientService().findPatient("psj", "1984-08-31", "Joshua", "Smith", aidbox.accessToken())

        val errors = response.errors
        assertTrue(errors == null || errors.isEmpty())
        val patients = response.data?.patientList ?: listOf()
        assertEquals(0, patients.size)
    }

    @Test
    @AidboxData("aidbox/patient2.yaml")
    fun `method level data mixed with class level so patient found`() {
        val response = patientService().findPatient("psj", "1984-08-31", "Joshua", "Smith", aidbox.accessToken())

        val errors = response.errors
        assertTrue(errors == null || errors.isEmpty())
        val patients = response.data?.patientList ?: listOf()
        assertEquals(1, patients.size)
        assertEquals("1984-08-31", patients[0].birthDate)
    }

    private fun patientService() = PatientService(
        AidboxClient(
            AidboxConfig().getHttpClient(),
            "${aidbox.baseUrl()}/\$graphql"
        )
    )
}
