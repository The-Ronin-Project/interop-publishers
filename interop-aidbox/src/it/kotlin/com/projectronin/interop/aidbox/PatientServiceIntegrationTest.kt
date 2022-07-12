package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.spring.AidboxIntegrationConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [AidboxIntegrationConfig::class])
@AidboxData("/aidbox/patient/Patients.yaml")
class PatientServiceIntegrationTest : BaseAidboxTest() {
    @Autowired
    private lateinit var patientService: PatientService

    companion object {
        // allows us to dynamically change the aidbox port to the testcontainer instance
        @JvmStatic
        @DynamicPropertySource
        fun aidboxUrlProperties(registry: DynamicPropertyRegistry) {
            registry.add("aidbox.url") { aidbox.baseUrl() }
            registry.add("aidbox.client.id") { aidbox.aidboxClientId }
            registry.add("aidbox.client.secret") { aidbox.aidboxClientSecret }
        }
    }

    @Test
    fun `no patient FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("1234", "unknown-system-1"),
            2 to SystemValue("5678", "unknown-system-2")
        )
        val fhirIds = patientService.getPatientFHIRIds("mdaoc", identifiers)
        assertTrue(fhirIds.isEmpty())
    }

    @Test
    fun `some patient FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("1234", "http://projectronin.com/id/mrn"),
            2 to SystemValue("5678", "unknown-system-2")
        )
        val fhirIds = patientService.getPatientFHIRIds("mdaoc", identifiers)
        assertEquals(1, fhirIds.size)
        assertEquals("mdaoc-12345678901", fhirIds[1])
    }

    @Test
    fun `all patient FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("1234", "http://projectronin.com/id/mrn"),
            2 to SystemValue("abcdef", "my-own-system")
        )
        val fhirIds = patientService.getPatientFHIRIds("mdaoc", identifiers)
        assertEquals(2, fhirIds.size)
        assertEquals("mdaoc-12345678901", fhirIds[1])
        assertEquals("mdaoc-12345678902", fhirIds[2])
    }

    @Test
    fun `identifiers are restricted to requested tenant when searching for patient FHIR Ids`() {
        val identifiers = mapOf(
            1 to SystemValue("1234", "http://projectronin.com/id/mrn"),
            2 to SystemValue("abcdef", "my-own-system")
        )
        val fhirIds = patientService.getPatientFHIRIds("newtenant", identifiers)
        assertTrue(fhirIds.isEmpty())
    }

    @Test
    fun `returns an empty map when retrieving all patients for an unknown tenant`() {
        val identifiersByFHIRId = patientService.getPatientsByTenant("unknown")
        assertTrue(identifiersByFHIRId.isEmpty())
    }

    @Test
    fun `retrieves all patients for a tenant`() {
        val identifiersByFHIRId = patientService.getPatientsByTenant("mdaoc")
        assertEquals(2, identifiersByFHIRId.size)

        val tenantIdentifier =
            Identifier(
                type = CodeableConcept(text = "Tenant ID"),
                system = CodeSystem.RONIN_TENANT.uri,
                value = "mdaoc"
            )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "MRN"),
                    system = Uri("http://projectronin.com/id/mrn"),
                    value = "1234"
                ),
                Identifier(
                    type = CodeableConcept(text = "FHIR STU3"),
                    system = Uri("http://projectronin.com/id/fhir"),
                    value = "12345678901"
                )
            ),
            identifiersByFHIRId["mdaoc-12345678901"]
        )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "MRN"),
                    system = Uri("http://projectronin.com/id/mrn"),
                    value = "5678"
                ),
                Identifier(
                    type = CodeableConcept(text = "FHIR STU3"),
                    system = Uri("http://projectronin.com/id/fhir"),
                    value = "12345678902"
                ),
                Identifier(
                    system = Uri("my-own-system"),
                    value = "abcdef"
                )
            ),
            identifiersByFHIRId["mdaoc-12345678902"]
        )
    }

    @Test
    fun `getFHIRIdsForTenant - success`() {
        val fhirIds = patientService.getPatientFHIRIdsByTenant("mdaoc")
        assertEquals(2, fhirIds.size)
        assertEquals("mdaoc-12345678901", fhirIds.get(0))
        assertEquals("mdaoc-12345678902", fhirIds.get(1))
    }

    @Test
    fun `getFHIRIdsForTenant - no data`() {
        val fhirIds = patientService.getPatientFHIRIdsByTenant("tenant")
        assertEquals(0, fhirIds.size)
        assertEquals(emptyList<String>(), fhirIds)
    }

    @Test
    fun `return and deserialize full patient`() {
        val patient = patientService.getOncologyPatient("mdaoc", "mdaoc-12345678901")
        assertEquals(patient.id?.value, "mdaoc-12345678901")
    }

    @Test
    fun `patient from a different tenant throws exception`() {
        assertThrows<Exception> { patientService.getOncologyPatient("newTenant", "mdaoc-12345678901") }
    }
}
