package com.projectronin.interop.aidbox.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.projectronin.interop.aidbox.auth.AidboxAuthenticationBroker
import com.projectronin.interop.aidbox.model.GraphQLPostRequest
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.FHIRResource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.LocationHoursOfOperation
import com.projectronin.interop.fhir.r4.datatype.LocationPosition
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitionerRole
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AidboxClientTest {
    private val urlRest = "http://localhost/8888"
    private val urlBatchUpsert = "$urlRest/fhir"
    private val practitioner1 = OncologyPractitioner(
        id = Id("cmjones"),
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "third"
            )
        ),
        name = listOf(HumanName(family = "Jones", given = listOf("Cordelia", "May"))),
    )
    private val practitioner2 = OncologyPractitioner(
        id = Id("rallyr"),
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "second"
            )
        ),
        name = listOf(HumanName(family = "Llyr", given = listOf("Regan", "Anne"))),
    )
    private val practitioner3 = OncologyPractitioner(
        id = Id("gwalsh"),
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "first"
            )
        ),
        name = listOf(HumanName(family = "Walsh", given = listOf("Goneril"))),
    )
    private val location1 = Location(
        id = Id("12345"),
        language = Code("en-US"),
        text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "id5"
            )
        ),
        mode = LocationMode.INSTANCE,
        status = LocationStatus.ACTIVE,
        name = "My Office",
        alias = listOf("Guest Room"),
        description = "Sun Room",
        type = listOf(
            CodeableConcept(
                text = "Diagnostic",
                coding = listOf(
                    Coding(
                        code = Code("DX"),
                        system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                    )
                )
            )
        ),
        telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
        address = Address(country = "USA"),
        physicalType = CodeableConcept(
            text = "Room",
            coding = listOf(
                Coding(
                    code = Code("ro"),
                    system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                )
            )
        ),
        position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132)),
        hoursOfOperation = listOf(
            LocationHoursOfOperation(
                daysOfWeek = listOf(
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                ),
                allDay = true
            )
        ),
        availabilityExceptions = "Call for details",
        endpoint = listOf(Reference(reference = "Endpoint/4321"))
    )
    private val location2 = Location(
        id = Id("12346"),
        language = Code("en-US"),
        text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "id6"
            )
        ),
        mode = LocationMode.INSTANCE,
        status = LocationStatus.ACTIVE,
        name = "Back Study",
        alias = listOf("Studio"),
        description = "Game Room",
        type = listOf(
            CodeableConcept(
                text = "Diagnostic",
                coding = listOf(
                    Coding(
                        code = Code("DX"),
                        system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                    )
                )
            )
        ),
        telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "123-456-7890")),
        address = Address(country = "USA"),
        physicalType = CodeableConcept(
            text = "Room",
            coding = listOf(
                Coding(
                    code = Code("ro"),
                    system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                )
            )
        ),
        hoursOfOperation = listOf(LocationHoursOfOperation(daysOfWeek = listOf(DayOfWeek.TUESDAY), allDay = true)),
        availabilityExceptions = "By appointment",
        endpoint = listOf(Reference(reference = "Endpoint/4322"))
    )
    private val practitionerRole1 = OncologyPractitionerRole(
        id = Id("12347"),
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "id3"
            )
        ),
        active = true,
        period = Period(end = DateTime("2022")),
        practitioner = Reference(reference = "Practitioner/cmjones"),
        location = listOf(Reference(reference = "Location/12345")),
        healthcareService = listOf(Reference(reference = "HealthcareService/3456")),
        availableTime = listOf(AvailableTime(allDay = false)),
        notAvailable = listOf(NotAvailable(description = "Not available now")),
        availabilityExceptions = "exceptions",
        endpoint = listOf(Reference(reference = "Endpoint/1357"))
    )
    private val practitionerRole2 = OncologyPractitionerRole(
        id = Id("12348"),
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "id4"
            )
        ),
        active = true,
        period = Period(end = DateTime("2022")),
        practitioner = Reference(reference = "Practitioner/rallyr"),
        location = listOf(Reference(reference = "Location/12346")),
        healthcareService = listOf(Reference(reference = "HealthcareService/3456")),
        availableTime = listOf(AvailableTime(allDay = true)),
        notAvailable = listOf(NotAvailable(description = "Available now")),
        availabilityExceptions = "No exceptions",
        endpoint = listOf(Reference(reference = "Endpoint/1358"), Reference(reference = "Endpoint/1359"))
    )
    private val practitioners = listOf(practitioner1, practitioner2)
    private val locations = listOf(location1, location2)
    private val practitionerRoles = listOf(practitionerRole1, practitionerRole2)
    private val oneMissingTargetRoles: List<FHIRResource> = practitioners + listOf(location1) + practitionerRoles
    private val fullRoles: List<FHIRResource> = practitioners + locations + practitionerRoles
    private val unrelatedResourceInList: List<FHIRResource> =
        listOf(practitioner3) + practitioners + locations + practitionerRoles

    @Test
    fun `query test`() {
        val query = javaClass.getResource("/graphql/AidboxLimitedPractitionerIDsQuery.graphql")!!.readText()
        val param = mapOf("id" to "id1")
        val expectedBody = objectMapper.writeValueAsString(GraphQLPostRequest(query = query, variables = param))
        println(expectedBody)
        val aidboxClient = createClient(expectedBody = expectedBody, expectedUrl = "$urlRest/\$graphql")
        val actual: HttpResponse = runBlocking {
            aidboxClient.queryGraphQL(query, param)
        }
        assertEquals(actual.status, HttpStatusCode.OK)
    }

    @Test
    fun `resource retrieve test`() {
        val aidboxClient = createClient("", "$urlRest/fhir/Patient/123")
        val actual: HttpResponse = runBlocking {
            aidboxClient.getResource("Patient", "123")
        }
        assertEquals(actual.status, HttpStatusCode.OK)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner (RoninResource), response 200`() {
        val expectedResponseStatus = HttpStatusCode.OK
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitioners)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Location (R4Resource), response 200`() {
        val expectedResponseStatus = HttpStatusCode.OK
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(locations)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of all related FHIRResources (RoninResource and R4Resource), response 200`() {
        val expectedResponseStatus = HttpStatusCode.OK
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(fullRoles)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of mixed, some unrelated FHIRResources (RoninResource and R4Resource), response 200`() {
        val expectedResponseStatus = HttpStatusCode.OK
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(unrelatedResourceInList)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of PractitionerRole with all reference targets missing, response 422`() {
        val expectedResponseStatus = HttpStatusCode.UnprocessableEntity
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(ClientRequestException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(practitionerRoles)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    @Test
    fun `aidbox batch upsert of PractitionerRole with only 1 reference target missing, response 422`() {
        val expectedResponseStatus = HttpStatusCode.UnprocessableEntity
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(ClientRequestException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(oneMissingTargetRoles)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 1xx`() {
        val expectedResponseStatus = HttpStatusCode.Continue
        val aidboxClient =
            createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitioners)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 2xx`() {
        val expectedResponseStatus = HttpStatusCode.Accepted
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitioners)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 3xx exception`() {
        val expectedResponseStatus = HttpStatusCode.TemporaryRedirect
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(RedirectResponseException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(practitioners)
            }
        }
        assertEquals(exception.response.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 4xx exception`() {
        val expectedResponseStatus = HttpStatusCode.Unauthorized
        val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(ClientRequestException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(practitioners)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 5xx exception`() {
        val expectedResponseStatus = HttpStatusCode.ServiceUnavailable
        val exception = assertThrows(ServerResponseException::class.java) {
            val aidboxClient = createClient(expectedUrl = urlBatchUpsert, responseStatus = expectedResponseStatus)
            runBlocking {
                aidboxClient.batchUpsert(practitioners)
            }
        }
        assertEquals(exception.response.status, expectedResponseStatus)
    }

    private fun createClient(
        expectedBody: String = "",
        expectedUrl: String,
        baseUrl: String = urlRest,
        responseStatus: HttpStatusCode = HttpStatusCode.OK,
    ): AidboxClient {
        val authenticationBroker = mockk<AidboxAuthenticationBroker> {
            every { getAuthentication() } returns mockk {
                every { tokenType } returns "Bearer"
                every { accessToken } returns "Auth-String"
            }
        }

        val mockEngine = MockEngine { request ->
            assertEquals(expectedUrl, request.url.toString())
            if (expectedBody != "") {
                assertEquals(expectedBody, String(request.body.toByteArray())) // see if this is a JSON string/stream
            }
            assertEquals("Bearer Auth-String", request.headers["Authorization"])
            respond(
                content = "",
                status = responseStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                jackson {
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                }
            }
            expectSuccess = true
        }

        return AidboxClient(httpClient, baseUrl, authenticationBroker)
    }
}
