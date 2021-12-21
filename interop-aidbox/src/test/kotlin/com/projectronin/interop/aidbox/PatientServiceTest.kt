package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.client.model.GraphQLError
import com.projectronin.interop.aidbox.client.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.AidboxAddress
import com.projectronin.interop.aidbox.model.AidboxContactPoint
import com.projectronin.interop.aidbox.model.AidboxHumanName
import com.projectronin.interop.aidbox.model.AidboxIdentifier
import com.projectronin.interop.aidbox.model.AidboxPatient
import com.projectronin.interop.aidbox.model.AidboxPatientList
import com.projectronin.interop.aidbox.utils.roninTenantNamespace
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PatientServiceTest {
    private val patientQuery =
        PatientServiceTest::class.java.getResource("/graphql/FindPatient.graphql").readText()

    private lateinit var aidboxClient: AidboxClient
    private lateinit var service: PatientService

    @BeforeEach
    fun initTest() {
        aidboxClient = mockk<AidboxClient>()
        service = PatientService(aidboxClient)
    }

    @Test
    fun `handles exception from AidboxClient`() {
        val httpResponse = mockk<HttpResponse>()
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.Unauthorized
        coEvery { httpResponse.receive<String>() } returns "Unauthorized"

        coEvery {
            aidboxClient.query(
                patientQuery,
                "auth-string",
                mapOf(
                    "tenant" to "${roninTenantNamespace()}|TENANT1",
                    "birthDate" to "1984-08-31",
                    "givenName" to "Josh",
                    "familyName" to "Smith"
                )
            )
        } throws ClientRequestException(httpResponse, "Error")

        val patientResponse =
            service.findPatient("TENANT1", "1984-08-31", "Josh", "Smith", "auth-string")

        val expectedError =
            GraphQLError("Error communicating with Aidbox. Received status code 401 Unauthorized with message \"Unauthorized\"")
        val expectedResponse = GraphQLResponse<String>(errors = listOf(expectedError))
        assertEquals(expectedResponse, patientResponse)
    }

    @Test
    fun `handles GraphQLResponse with no errors`() {
        val identifier = AidboxIdentifier(system = "http://hl7.org/fhir/sid/us-ssn", value = "987-65-4321")
        val name = AidboxHumanName(use = "official", family = "Smith", given = listOf("Josh"))
        val contact = AidboxContactPoint(system = "phone", use = "mobile", value = "123-456-7890")
        val address = AidboxAddress(
            use = "home",
            line = listOf("1234 Main St"),
            city = "Anywhere",
            state = "FL",
            postalCode = "37890"
        )
        val patient = AidboxPatient(
            id = "Patient-UUID-1",
            identifier = listOf(identifier),
            name = listOf(name),
            birthDate = "1984-08-31",
            gender = "male",
            telecom = listOf(contact),
            address = listOf(address)
        )
        val response = GraphQLResponse<AidboxPatientList>(data = AidboxPatientList(listOf(patient)))

        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.receive<GraphQLResponse<AidboxPatientList>>() } returns response

        coEvery {
            aidboxClient.query(
                patientQuery,
                "auth-string",
                mapOf(
                    "tenant" to "${roninTenantNamespace()}|TENANT1",
                    "birthDate" to "1984-08-31",
                    "givenName" to "Josh",
                    "familyName" to "Smith"
                )
            )
        } returns httpResponse

        val patientResponse =
            service.findPatient("TENANT1", "1984-08-31", "Josh", "Smith", "auth-string")
        assertEquals(response, patientResponse)
    }

    @Test
    fun `handles GraphQLResponse with errors`() {
        val patient = AidboxPatient(id = "Patient-UUID-1", name = listOf(AidboxHumanName(given = listOf("Josh"))))
        val error = GraphQLError("Error occurred")
        val response =
            GraphQLResponse<AidboxPatientList>(data = AidboxPatientList(listOf(patient)), errors = listOf(error))

        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.receive<GraphQLResponse<AidboxPatientList>>() } returns response

        coEvery {
            aidboxClient.query(
                patientQuery,
                "auth-string",
                mapOf(
                    "tenant" to "${roninTenantNamespace()}|TENANT1",
                    "birthDate" to "1984-08-31",
                    "givenName" to "Josh",
                    "familyName" to "Smith"
                )
            )
        } returns httpResponse

        val patientResponse =
            service.findPatient("TENANT1", "1984-08-31", "Josh", "Smith", "auth-string")
        assertEquals(response, patientResponse)
    }
}
