package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.spring.AidboxSpringConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
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
@ContextConfiguration(classes = [AidboxSpringConfig::class])
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
            1 to SystemValue("1234", CodeSystem.RONIN_MRN.uri.value!!),
            2 to SystemValue("5678", "unknown-system-2")
        )
        val fhirIds = patientService.getPatientFHIRIds("mdaoc", identifiers)
        assertEquals(1, fhirIds.size)
        assertEquals("12345678901", fhirIds[1])
    }

    @Test
    fun `all patient FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("1234", CodeSystem.RONIN_MRN.uri.value!!),
            2 to SystemValue("abcdef", "my-own-system")
        )
        val fhirIds = patientService.getPatientFHIRIds("mdaoc", identifiers)
        assertEquals(2, fhirIds.size)
        assertEquals("12345678901", fhirIds[1])
        assertEquals("12345678902", fhirIds[2])
    }

    @Test
    fun `identifiers are restricted to requested tenant when searching for patient FHIR Ids`() {
        val identifiers = mapOf(
            1 to SystemValue("1234", CodeSystem.RONIN_MRN.uri.value!!),
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
                type = CodeableConcept(text = "Tenant ID".asFHIR()),
                system = CodeSystem.RONIN_TENANT.uri,
                value = "mdaoc".asFHIR()
            )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "MRN".asFHIR()),
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "1234".asFHIR()
                ),
                Identifier(
                    type = CodeableConcept(text = "FHIR STU3".asFHIR()),
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345678901".asFHIR()
                )
            ),
            identifiersByFHIRId["12345678901"]
        )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "MRN".asFHIR()),
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "5678".asFHIR()
                ),
                Identifier(
                    type = CodeableConcept(text = "FHIR STU3".asFHIR()),
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345678902".asFHIR()
                ),
                Identifier(
                    system = Uri("my-own-system"),
                    value = "abcdef".asFHIR()
                )
            ),
            identifiersByFHIRId["12345678902"]
        )
    }

    @Test
    fun `getFHIRIdsForTenant - success`() {
        val fhirIds = patientService.getPatientFHIRIdsByTenant("mdaoc")
        assertEquals(2, fhirIds.size)
        assertEquals("12345678901", fhirIds.get(0))
        assertEquals("12345678902", fhirIds.get(1))
    }

    @Test
    fun `getFHIRIdsForTenant - no data`() {
        val fhirIds = patientService.getPatientFHIRIdsByTenant("tenant")
        assertEquals(0, fhirIds.size)
        assertEquals(emptyList<String>(), fhirIds)
    }

    @Test
    fun `return and deserialize full patient`() {
        val patient = patientService.getPatientByUDPId("mdaoc", "mdaoc-12345678901")
        assertEquals(patient.id?.value, "mdaoc-12345678901")
    }

    @Test
    fun `patient error`() {
        assertThrows<ClientFailureException> { patientService.getPatientByUDPId("mdaoc", "mdaoc-12345") }
    }

    @Test
    fun `patient from a different tenant throws exception`() {
        assertThrows<Exception> { patientService.getPatientByUDPId("newTenant", "mdaoc-12345678901") }
    }
}
