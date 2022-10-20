package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.spring.AidboxIntegrationConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
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
@AidboxData("/aidbox/practitioner/Practitioners.yaml")
class PractitionerServiceIntegrationTest : BaseAidboxTest() {
    @Autowired
    private lateinit var practitionerService: PractitionerService

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
    fun `get identifiers for unknown practitioner FHIR ID`() {
        val identifiers = practitionerService.getPractitionerIdentifiers("mdaoc", "mdaoc-unknown-fhir-id")
        assertTrue(identifiers.isEmpty())
    }

    @Test
    fun `get identifiers for known practitioner FHIR ID`() {
        val identifiers = practitionerService.getPractitionerIdentifiers("mdaoc", "mdaoc-ef9TegF2nfECi-0Skirbvpg3")
        assertEquals(10, identifiers.size)

        val identifier1 = createIdentifier("http://projectronin.com/id/tenantId", "mdaoc", "Tenant ID")
        assertTrue(identifier1 in identifiers)
        val identifier2 = createIdentifier("http://hl7.org/fhir/sid/us-npi", "1100399991", "NPI")
        assertTrue(identifier2 in identifiers)
        val identifier3 = createIdentifier("urn:oid:1.2.840.114350.1.13.0.1.7.2.697780", "CARDMD", "INTERNAL")
        assertTrue(identifier3 in identifiers)
        val identifier4 = createIdentifier("urn:oid:1.2.840.114350.1.13.0.1.7.2.697780", "CARDMD", "EXTERNAL")
        assertTrue(identifier4 in identifiers)
        val identifier5 = createIdentifier("PROVID", "1003", "PROVID")
        assertTrue(identifier5 in identifiers)
        val identifier6 = createIdentifier("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.7", "207RC0000X", "EPIC")
        assertTrue(identifier6 in identifiers)
        val identifier7 = createIdentifier("urn:oid:2.16.840.1.113883.4.6", "1100399991", "NPI")
        assertTrue(identifier7 in identifiers)
        val identifier8 = createIdentifier("urn:oid:1.2.840.114350.1.13.0.1.7.5.737384.63", "TEMP1003", "Epic")
        assertTrue(identifier8 in identifiers)
        val identifier9 = createIdentifier("urn:oid:1.2.840.114350.1.13.0.1.7.2.836982", "E1003", "INTERNAL")
        assertTrue(identifier9 in identifiers)
        val identifier10 = createIdentifier("urn:oid:1.2.840.114350.1.13.0.1.7.2.836982", "E1003", "EXTERNAL")
        assertTrue(identifier10 in identifiers)
    }

    @Test
    fun `get identifiers does not return known practitioner from different tenant`() {
        val identifiers = practitionerService.getPractitionerIdentifiers("newtenant", "mdaoc-unknown-fhir-id")
        assertTrue(identifiers.isEmpty())
    }

    @Test
    fun `no practitioner FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("external-value", "unknown-system-1"),
            2 to SystemValue("internal-value2", "unknown-system-2")
        )
        val fhirIds = practitionerService.getPractitionerFHIRIds("tenant", identifiers)
        assertTrue(fhirIds.isEmpty())
    }

    @Test
    fun `some practitioner FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("external-value", "external-system"),
            2 to SystemValue("internal-value2", "unknown-system-2")
        )
        val fhirIds = practitionerService.getPractitionerFHIRIds("tenant", identifiers)
        assertEquals(1, fhirIds.size)
        assertEquals("tenant-practitioner1", fhirIds[1])
    }

    @Test
    fun `all practitioner FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("external-value", "external-system"),
            2 to SystemValue("internal-value2", "internal-system")
        )
        val fhirIds = practitionerService.getPractitionerFHIRIds("tenant", identifiers)
        assertEquals(2, fhirIds.size)
        assertEquals("tenant-practitioner1", fhirIds[1])
        assertEquals("tenant-practitioner2", fhirIds[2])
    }

    @Test
    fun `identifiers are restricted to requested tenant when searching for practitioner FHIR Ids`() {
        val identifiers = mapOf(
            1 to SystemValue("external-value", "external-system"),
            2 to SystemValue("internal-value2", "internal-system")
        )
        val fhirIds = practitionerService.getPractitionerFHIRIds("newtenant", identifiers)
        assertTrue(fhirIds.isEmpty())
    }

    @Test
    fun `returns an empty map when retrieving all practitioners for an unknown tenant`() {
        val identifiersByFHIRId = practitionerService.getPractitionersByTenant("unknown")
        assertTrue(identifiersByFHIRId.isEmpty())
    }

    @Test
    fun `retrieves all practitioners for a tenant`() {
        val identifiersByFHIRId = practitionerService.getPractitionersByTenant("tenant")
        assertEquals(2, identifiersByFHIRId.size)

        val tenantIdentifier =
            Identifier(
                type = CodeableConcept(text = "Tenant ID"),
                system = Uri("http://projectronin.com/id/tenantId"),
                value = "tenant"
            )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "EXTERNAL"),
                    system = Uri("external-system"),
                    value = "external-value"
                ),
                Identifier(
                    type = CodeableConcept(text = "INTERNAL"),
                    system = Uri("internal-system"),
                    value = "internal-value"
                )
            ),
            identifiersByFHIRId["tenant-practitioner1"]
        )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "EXTERNAL"),
                    system = Uri("external-system"),
                    value = "external-value2"
                ),
                Identifier(
                    type = CodeableConcept(text = "INTERNAL"),
                    system = Uri("internal-system"),
                    value = "internal-value2"
                )
            ),
            identifiersByFHIRId["tenant-practitioner2"]
        )
    }

    @Test
    fun `retrieves all practitioners for a tenant when multiple pages are needed`() {
        // crazy low batch size, but we only have 2 practitioners in, so need to set to 1 to prove out.
        val identifiersByFHIRId = practitionerService.getPractitionersByTenant("tenant", 1)
        assertEquals(2, identifiersByFHIRId.size)

        val tenantIdentifier =
            Identifier(
                type = CodeableConcept(text = "Tenant ID"),
                system = Uri("http://projectronin.com/id/tenantId"),
                value = "tenant"
            )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "EXTERNAL"),
                    system = Uri("external-system"),
                    value = "external-value"
                ),
                Identifier(
                    type = CodeableConcept(text = "INTERNAL"),
                    system = Uri("internal-system"),
                    value = "internal-value"
                )
            ),
            identifiersByFHIRId["tenant-practitioner1"]
        )
        assertEquals(
            listOf(
                tenantIdentifier,
                Identifier(
                    type = CodeableConcept(text = "EXTERNAL"),
                    system = Uri("external-system"),
                    value = "external-value2"
                ),
                Identifier(
                    type = CodeableConcept(text = "INTERNAL"),
                    system = Uri("internal-system"),
                    value = "internal-value2"
                )
            ),
            identifiersByFHIRId["tenant-practitioner2"]
        )
    }

    @Test
    fun `return and deserialize full practitioner`() {
        val practitioner = practitionerService.getPractitioner("mdaoc", "mdaoc-ef9TegF2nfECi-0Skirbvpg3")
        assertEquals(practitioner.id?.value, "mdaoc-ef9TegF2nfECi-0Skirbvpg3")
    }

    @Test
    fun `practitioner from a different tenant throws exception`() {
        assertThrows<Exception> {
            practitionerService.getPractitioner("newTenant", "mdaoc-ef9TegF2nfECi-0Skirbvpg3")
        }
    }

    private fun createIdentifier(system: String, value: String, typeText: String) =
        Identifier(type = CodeableConcept(text = typeText), system = Uri(system), value = value)
}
