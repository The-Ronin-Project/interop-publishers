package com.projectronin.interop.aidbox.testcontainer

import com.projectronin.interop.aidbox.testcontainer.client.PatientService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JUnit tests focused on ensuring flows related to method-level data work appropriately.
 */
class AidboxContainerMethodDataTest : BaseAidboxTest() {
    @Test
    @AidboxData("aidbox/patient1.yaml")
    fun `single data file with patient found`() {
        val response = patientService().findPatient("mdaoc", "1984-08-31", "Joshua", "Smith", aidbox.accessToken())

        val errors = response.errors
        assertTrue(errors == null || errors.isEmpty())
        val patients = response.data?.patientList ?: listOf()
        assertEquals(1, patients.size)
        assertEquals("1984-08-31", patients[0].birthDate)
    }

    @Test
    @AidboxData("aidbox/patient2.yaml")
    fun `single data file with patient not found`() {
        val response = patientService().findPatient("mdaoc", "1984-08-31", "Joshua", "Smith", aidbox.accessToken())

        val errors = response.errors
        assertTrue(errors == null || errors.isEmpty())
        val patients = response.data?.patientList ?: listOf()
        assertEquals(0, patients.size)
    }

    @Test
    @AidboxData("/aidbox/patient1.yaml", "/aidbox/patient2.yaml")
    fun `multiple data files with patient found`() {
        val response = patientService().findPatient("mdaoc", "1984-08-31", "Joshua", "Smith", aidbox.accessToken())

        val errors = response.errors
        assertTrue(errors == null || errors.isEmpty())
        val patients = response.data?.patientList ?: listOf()
        assertEquals(1, patients.size)
        assertEquals("1984-08-31", patients[0].birthDate)
    }

    private fun patientService() =
        PatientService(com.projectronin.interop.aidbox.testcontainer.client.AidboxClient("${aidbox.baseUrl()}/\$graphql"))
}
