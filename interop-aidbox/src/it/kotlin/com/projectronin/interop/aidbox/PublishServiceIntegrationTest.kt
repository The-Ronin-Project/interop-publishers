package com.projectronin.interop.aidbox

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.projectronin.interop.aidbox.spring.AidboxIntegrationConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import com.projectronin.interop.fhir.r4.ronin.resource.RoninDomainResource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [AidboxIntegrationConfig::class])
class PublishServiceIntegrationTest : BaseAidboxTest() {
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

    @Autowired
    private lateinit var publishService: PublishService

    @Test
    fun `can publish a new resource`() {
        // Verify that the resource does not exist.
        val initialResource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-new-resource")
        assertNull(initialResource)

        val practitioner = OncologyPractitioner(
            id = Id("mdaoc-new-resource"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Smith", given = listOf("Josh")))
        )
        val published = publishService.publish(listOf(practitioner))
        assertTrue(published)

        val resource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-new-resource")
        assertEquals(practitioner, resource)
    }

    @Test
    @AidboxData("/aidbox/publish/PractitionerToUpdate.yaml")
    fun `can publish an updated resource`() {
        val practitioner = OncologyPractitioner(
            id = Id("mdaoc-existing-resource"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Doctor", given = listOf("Bob")))
        )

        // Verify that the resource does exist.
        val initialResource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-existing-resource")
        assertNotNull(initialResource)
        // And that it isn't what we are about to make it.
        assertNotEquals(practitioner, initialResource)

        val published = publishService.publish(listOf(practitioner))
        assertTrue(published)

        val resource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-existing-resource")
        assertEquals(practitioner, resource)
    }

    @Test
    fun `can publish multiple resources of the same type`() {
        val practitioner1 = OncologyPractitioner(
            id = Id("mdaoc-practitioner1"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Doctor", given = listOf("Bob")))
        )
        val practitioner2 = OncologyPractitioner(
            id = Id("mdaoc-practitioner2"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Public", given = listOf("John", "Q")))
        )

        val published = publishService.publish(listOf(practitioner1, practitioner2))
        assertTrue(published)

        val resource1 = getResource<OncologyPractitioner>("Practitioner", "mdaoc-practitioner1")
        assertEquals(practitioner1, resource1)

        val resource2 = getResource<OncologyPractitioner>("Practitioner", "mdaoc-practitioner2")
        assertEquals(practitioner2, resource2)
    }

    @Test
    fun `can publish multiple resources of different types`() {
        val practitioner = OncologyPractitioner(
            id = Id("mdaoc-practitioner"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Doctor", given = listOf("Bob")))
        )
        val patient = OncologyPatient(
            id = Id("mdaoc-patient"),
            identifier = listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "mdaoc"),
                Identifier(type = CodeableConcepts.MRN, system = CodeSystem.MRN.uri, value = "1234"),
                Identifier(
                    type = CodeableConcepts.FHIR_STU3_ID,
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    value = "patient"
                )
            ),
            name = listOf(HumanName(family = "Doe", given = listOf("John"))),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.EMAIL,
                    value = "john.doe@projectronin.com",
                    use = ContactPointUse.WORK
                )
            ),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1976-02-16"),
            address = listOf(Address(text = "Address")),
            maritalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus"),
                        code = Code("U")
                    )
                ),
                text = "Unmarried"
            )
        )

        val published = publishService.publish(listOf(practitioner, patient))
        assertTrue(published)

        val resource1 = getResource<OncologyPractitioner>("Practitioner", "mdaoc-practitioner")
        assertEquals(practitioner, resource1)

        val resource2 = getResource<OncologyPatient>("Patient", "mdaoc-patient")
        assertEquals(patient, resource2)
    }

    private inline fun <reified T : RoninDomainResource> getResource(resourceType: String, id: String): T? {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val response = try {
                aidbox.ktorClient.get<HttpResponse>("$aidboxUrl/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
                e.response
            }

            if (response.status.isSuccess()) {
                val objectNode = response.receive<ObjectNode>()
                // Remove meta since Aidbox sets it.
                objectNode.remove("meta")

                objectMapper.convertValue<T>(objectNode)
            } else if (response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw IllegalStateException("Error while purging test data: ${response.receive<String>()}")
            }
        }
    }
}
