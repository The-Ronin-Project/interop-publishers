package com.projectronin.interop.aidbox

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.exception.InvalidTenantAccessException
import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PatientServiceTest {
    private val aidboxClient = mockk<AidboxClient>()
    private val patientService = PatientService(aidboxClient, 2)
    private val query = javaClass.getResource("/graphql/AidboxPatientFHIRIDsQuery.graphql")!!.readText()
    private val patientListQuery = javaClass.getResource("/graphql/PatientListQuery.graphql")!!.readText()

    private val tenantMnemonic = "mdaoc"
    private val mrn1 = "01111"
    private val mrn2 = "01112"

    private val tenantQueryString = "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic"
    private val tenantIdentifier = Identifier(system = CodeSystem.RONIN_TENANT.uri, value = tenantMnemonic)

    private val mrnSystemValue1 = SystemValue(system = CodeSystem.MRN.uri.value, value = mrn1)
    private val mrnIdentifier1 = Identifier(system = CodeSystem.MRN.uri, value = mrn1)

    private val mrnSystemValue2 = SystemValue(system = CodeSystem.MRN.uri.value, value = mrn2)
    private val mrnIdentifier2 = Identifier(system = CodeSystem.MRN.uri, value = mrn2)

    private val mockPatientIdentifiers1 = LimitedPatientIdentifiers(
        id = "roninPatient01Test",
        identifiers = listOf(
            tenantIdentifier,
            mrnIdentifier1
        )
    )
    private val mockPatientIdentifiers2 = LimitedPatientIdentifiers(
        id = "roninPatient02Test",
        identifiers = listOf(
            tenantIdentifier,
            mrnIdentifier2
        )
    )
    private val mockPatientList = PatientList(
        patientList = listOf(
            PartialPatient(
                identifier = listOf(
                    Identifier(value = "mdaoc", type = CodeableConcepts.RONIN_TENANT),
                    Identifier(value = "22221", type = CodeableConcepts.SER),
                    Identifier(value = "9988776655")
                ),
                id = Id("mdaoc-123"),
            ),
            PartialPatient(
                identifier = listOf(
                    Identifier(value = "mdaoc", type = CodeableConcepts.RONIN_TENANT),
                    Identifier(value = "22222", type = CodeableConcepts.SER),
                    Identifier(value = "2281376654")
                ),
                id = Id("mdaoc-456"),
            )
        )
    )

    @BeforeEach
    fun setup() {
        mockkStatic("io.ktor.client.statement.HttpResponseKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("io.ktor.client.statement.HttpResponseKt")
    }

    @Test
    fun `getPatientFHIRIds returns all patients`() {
        val response = GraphQLResponse(
            data = LimitedPatient(listOf(mockPatientIdentifiers1, mockPatientIdentifiers2))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse.body() } returns response

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(2, actualMap.size)
        assertEquals(mockPatientIdentifiers1.id, actualMap["1"])
        assertEquals(mockPatientIdentifiers2.id, actualMap["2"])
    }

    @Test
    fun `getPatientFHIRIds returns some patients`() {
        val response = GraphQLResponse(
            data = LimitedPatient(listOf(mockPatientIdentifiers1))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse.body() } returns response

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(1, actualMap.size)
        assertEquals(mockPatientIdentifiers1.id, actualMap["1"])
    }

    @Test
    fun `getPatientFHIRIds returns no patients`() {
        val response = GraphQLResponse(
            data = LimitedPatient(listOf())
        )
        val mockHttpResponse = mockk<HttpResponse>()

        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse.body() } returns response

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getPatientFHIRIds returns GraphQL errors`() {
        val response = GraphQLResponse<LimitedPatient>(
            errors = listOf(GraphQLError("GraphQL Error"))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse.body() } returns response

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getPatientFHIRIds throws a ResponseException`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")

        mockkStatic("io.ktor.client.statement.HttpResponseKt")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } throws ClientFailureException(HttpStatusCode.ServiceUnavailable, "")

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(0, actualMap.size)

        unmockkStatic("io.ktor.client.statement.HttpResponseKt")
    }

    @Test
    fun `getPatientFHIRIds throws a non-response exception`() {
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } throws Exception("not a ResponseException")

        val exception = assertThrows<Exception> {
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))
        }
        assertEquals(exception.message, "not a ResponseException")
    }

    @Test
    fun `can deserialize actual Aidbox LimitedPatientIdentifiers JSON`() {
        val actualJson = """
            {
            "id": "roninPatient01Test",
            "identifier": [
              {
                "system": "http://projectronin.com/id/tenantId",
                "value": "mdaoc"
              },
              {
                "system": "http://projectronin.com/id/mrn",
                "value": "01111"
              },
              {
                "system": "http://projectronin.com/id/fhir",
                "value": "stu3-01111"
              }
            ]
          }
        """.trimIndent()
        val deserializedLimitedPatientIdentifiers = objectMapper.readValue<LimitedPatientIdentifiers>(actualJson)

        assertEquals(deserializedLimitedPatientIdentifiers.id, "roninPatient01Test")
        assertEquals(deserializedLimitedPatientIdentifiers.identifiers.size, 3)

        val identifier1 = deserializedLimitedPatientIdentifiers.identifiers[0]
        assertEquals(identifier1.system?.value, "http://projectronin.com/id/tenantId")
        assertEquals(identifier1.value, "mdaoc")

        val identifier2 = deserializedLimitedPatientIdentifiers.identifiers[1]
        assertEquals(identifier2.system?.value, "http://projectronin.com/id/mrn")
        assertEquals(identifier2.value, "01111")

        val identifier3 = deserializedLimitedPatientIdentifiers.identifiers[2]
        assertEquals(identifier3.system?.value, "http://projectronin.com/id/fhir")
        assertEquals(identifier3.value, "stu3-01111")
    }

    @Test
    fun `can deserialize actual Aidbox LimitedPatient JSON`() {
        val actualJson = """
            {
            "PatientList": [
              {
                "id": "roninPatient01Test",
                "identifier": [
                  {
                    "system": "http://projectronin.com/id/tenantId",
                    "value": "mdaoc"
                  },
                  {
                    "system": "http://projectronin.com/id/mrn",
                    "value": "01111"
                  },
                  {
                    "system": "http://projectronin.com/id/fhir",
                    "value": "stu3-01111"
                  }
                ]
              },
              {
                "id": "roninPatient02Test",
                "identifier": [
                  {
                    "system": "http://projectronin.com/id/tenantId",
                    "value": "mdaoc"
                  },
                  {
                    "system": "http://projectronin.com/id/mrn",
                    "value": "01112"
                  },
                  {
                    "system": "http://projectronin.com/id/fhir",
                    "value": "stu3-01112"
                  }
                ]
              }
            ]
          }
        """.trimIndent()
        val deserializedLimitedPatient = objectMapper.readValue<LimitedPatient>(actualJson)

        assertEquals(deserializedLimitedPatient.patientList?.size, 2)
    }

    @Test
    fun `getPatientsByTenant - success`() {
        val response = GraphQLResponse(data = mockPatientList)
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientList>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientsByTenant(tenantMnemonic)
        val fhirIds = response.data?.patientList?.map {
            it.id.value
        }
        assertEquals(fhirIds, actual.keys.toList())
    }

    @Test
    fun `getPatientsByTenant - no data`() {
        val response = GraphQLResponse(data = PatientList(listOf()))
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientList>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientsByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }

    @Test
    fun `getPatientsByTenant - exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientList>> { mockHttpResponse.body() } throws Exception()

        assertThrows<Exception> { patientService.getPatientsByTenant(tenantMnemonic) }
    }

    @Test
    fun `getPatientsByTenant - response exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"
        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } throws ClientFailureException(HttpStatusCode.ServiceUnavailable, "")

        val actual = patientService.getPatientsByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }

    @Test
    fun `getPatientFHIRIds returns all batched patients`() {
        val mrnSystemValue3 = SystemValue(system = CodeSystem.MRN.uri.value, value = "01113")
        val mrnIdentifier3 = Identifier(system = CodeSystem.MRN.uri, value = "01113")

        val mockPatientIdentifiers3 = LimitedPatientIdentifiers(
            id = "roninPatient03Test",
            identifiers = listOf(
                tenantIdentifier,
                mrnIdentifier3
            )
        )
        val response1 = GraphQLResponse(
            data = LimitedPatient(listOf(mockPatientIdentifiers1, mockPatientIdentifiers2))
        )

        val mockHttpResponse1 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse1
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse1.body() } returns response1

        val response2 = GraphQLResponse(
            data = LimitedPatient(listOf(mockPatientIdentifiers3))
        )

        val mockHttpResponse2 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue3).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse2
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse2.body() } returns response2

        val actualMap =
            patientService.getPatientFHIRIds(
                tenantMnemonic,
                mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2, "3" to mrnSystemValue3)
            )

        assertEquals(3, actualMap.size)
        assertEquals(mockPatientIdentifiers1.id, actualMap["1"])
        assertEquals(mockPatientIdentifiers2.id, actualMap["2"])
        assertEquals(mockPatientIdentifiers3.id, actualMap["3"])
    }

    @Test
    fun `getOncologyPatient - success`() {
        val httpMock = mockk<HttpResponse>()
        val patientMock = mockk<Patient>()
        every { patientMock.identifier } returns listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                value = tenantMnemonic
            )
        )
        coEvery { httpMock.body<Patient>() } returns patientMock
        coEvery { aidboxClient.getResource("Patient", "123") } returns httpMock
        val actual = patientService.getPatient(tenantMnemonic, "123")
        assertEquals(patientMock, actual)
    }

    @Test
    fun `getPatient - fails with non-matching tenant`() {
        val httpMock = mockk<HttpResponse>()
        val patientMock = mockk<Patient>()
        every { patientMock.identifier } returns listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                value = tenantMnemonic
            )
        )
        coEvery { httpMock.body<Patient>() } returns patientMock
        coEvery { aidboxClient.getResource("Patient", "123") } returns httpMock
        assertThrows<InvalidTenantAccessException> { patientService.getPatient("newTenant", "123") }
    }

    @Test
    fun `graphql errors are properly parsed for getPatientsByTenant`() {
        val mockHttpResponse = mockk<HttpResponse>()
        val data = this::class.java.getResource("/FailedPatientQuery.json")!!.readText()
        val response = objectMapper.readValue<GraphQLResponse<PatientList>>(data)

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientList>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientsByTenant(tenantMnemonic)
        assertEquals(emptyMap<String, List<Identifier>>(), actual)
    }

    @Test
    fun `graphql errors are properly parsed for getPatientFHIRIds`() {
        val mockHttpResponse = mockk<HttpResponse>()
        val data = this::class.java.getResource("/FailedPatientQuery.json")!!.readText()
        val response2 = objectMapper.readValue<GraphQLResponse<LimitedPatient>>(data)
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse.body() } returns response2

        val mrnSystemValue3 = SystemValue(system = CodeSystem.MRN.uri.value, value = "01113")

        val response1 = GraphQLResponse(
            data = LimitedPatient(listOf(mockPatientIdentifiers1, mockPatientIdentifiers2))
        )

        val mockHttpResponse1 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue1, mrnSystemValue2).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse1
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse1.body() } returns response1

        val mockHttpResponse2 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(mrnSystemValue3).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse2
        coEvery<GraphQLResponse<LimitedPatient>> { mockHttpResponse2.body() } returns response2

        val actualMap =
            patientService.getPatientFHIRIds(
                tenantMnemonic,
                mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2, "3" to mrnSystemValue3)
            )

        assertEquals(2, actualMap.size)
        assertEquals(mockPatientIdentifiers1.id, actualMap["1"])
        assertEquals(mockPatientIdentifiers2.id, actualMap["2"])
    }

    @Test
    fun `getFHIRIdsForTenant - success`() {
        val response = GraphQLResponse(data = mockPatientList)
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientList>> { mockHttpResponse.body() } returns response

        val expected = listOf("mdaoc-123", "mdaoc-456")
        val actual = patientService.getPatientFHIRIdsByTenant(tenantMnemonic)

        assertEquals(expected, actual)
    }

    @Test
    fun `getFHIRIdsForTenant - no data`() {
        val response = GraphQLResponse(data = PatientList(listOf()))
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientList>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientFHIRIdsByTenant(tenantMnemonic)

        assertEquals(emptyList<String>(), actual)
    }

    @Test
    fun `getFHIRIdsForTenant - exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientList>> { mockHttpResponse.body() } throws Exception()

        assertThrows<Exception> { patientService.getPatientFHIRIdsByTenant(tenantMnemonic) }
    }

    @Test
    fun `getFHIRIdsForTenant - response exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"
        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } throws ClientFailureException(HttpStatusCode.ServiceUnavailable, "")

        val actual = patientService.getPatientFHIRIdsByTenant(tenantMnemonic)

        assertEquals(emptyList<String>(), actual)
    }
}
