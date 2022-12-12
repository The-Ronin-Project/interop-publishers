package com.projectronin.interop.aidbox

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.exception.InvalidTenantAccessException
import com.projectronin.interop.aidbox.model.AidboxIdentifiers
import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.AIDBOX_PATIENT_FHIR_IDS_QUERY
import com.projectronin.interop.aidbox.utils.AIDBOX_PATIENT_LIST_QUERY
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
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
    private val query = AIDBOX_PATIENT_FHIR_IDS_QUERY
    private val patientListQuery = AIDBOX_PATIENT_LIST_QUERY
    private val tenantMnemonic = "mdaoc"
    private val mrn1 = "01111"
    private val mrn2 = "01112"

    private val tenantQueryString = "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic"
    private val tenantIdentifier = Identifier(system = CodeSystem.RONIN_TENANT.uri, value = tenantMnemonic.asFHIR())

    private val mrnSystemValue1 = SystemValue(system = "mrnSystem", value = mrn1)
    private val mrnIdentifier1 = Identifier(system = Uri("mrnSystem"), value = mrn1.asFHIR())
    private val fhirIdentifier1 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId1".asFHIR())

    private val mrnSystemValue2 = SystemValue(system = "mrnSystem", value = mrn2)
    private val mrnIdentifier2 = Identifier(system = Uri("mrnSystem"), value = mrn2.asFHIR())
    private val fhirIdentifier2 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId2".asFHIR())

    private val mockPatientIdentifiers1 = AidboxIdentifiers(
        udpId = "udpId1",
        identifiers = listOf(
            tenantIdentifier,
            mrnIdentifier1,
            fhirIdentifier1
        )
    )
    private val mockPatientIdentifiers2 = AidboxIdentifiers(
        udpId = "udpId2",
        identifiers = listOf(
            tenantIdentifier,
            mrnIdentifier2,
            fhirIdentifier2
        )
    )
    private val mockPatientsIdentifiers = PatientsIdentifiers(
        patientList = listOf(
            AidboxIdentifiers(
                identifiers = listOf(
                    Identifier(value = "mdaoc".asFHIR()),
                    Identifier(value = "22221".asFHIR()),
                    Identifier(value = "9988776655".asFHIR()),
                    Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "123".asFHIR())
                ),
                udpId = "mdaoc-123",
            ),
            AidboxIdentifiers(
                identifiers = listOf(
                    Identifier(value = "mdaoc".asFHIR()),
                    Identifier(value = "22222".asFHIR()),
                    Identifier(value = "2281376654".asFHIR()),
                    Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "456".asFHIR())
                ),
                udpId = "mdaoc-456",
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
            data = PatientsIdentifiers(listOf(mockPatientIdentifiers1, mockPatientIdentifiers2))
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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(2, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
        assertEquals("fhirId2", actualMap["2"])
    }

    @Test
    fun `getPatientFHIRIds returns some patients`() {
        val response = GraphQLResponse(
            data = PatientsIdentifiers(listOf(mockPatientIdentifiers1))
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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(1, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
    }

    @Test
    fun `getPatientFHIRIds returns no patients`() {
        val response = GraphQLResponse(
            data = PatientsIdentifiers(listOf())
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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap =
            patientService.getPatientFHIRIds(tenantMnemonic, mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2))

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getPatientFHIRIds returns GraphQL errors`() {
        val response = GraphQLResponse<PatientsIdentifiers>(
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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

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
        val deserializedAidboxIdentifiers = objectMapper.readValue<AidboxIdentifiers>(actualJson)

        assertEquals(deserializedAidboxIdentifiers.udpId, "roninPatient01Test")
        assertEquals(deserializedAidboxIdentifiers.identifiers.size, 3)

        val identifier1 = deserializedAidboxIdentifiers.identifiers[0]
        assertEquals(CodeSystem.RONIN_TENANT.uri.value, identifier1.system?.value)
        assertEquals("mdaoc".asFHIR(), identifier1.value)

        val identifier2 = deserializedAidboxIdentifiers.identifiers[1]
        assertEquals(CodeSystem.RONIN_MRN.uri.value!!, identifier2.system?.value)
        assertEquals("01111".asFHIR(), identifier2.value)

        val identifier3 = deserializedAidboxIdentifiers.identifiers[2]
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri.value!!, identifier3.system?.value)
        assertEquals("stu3-01111".asFHIR(), identifier3.value)
    }

    @Test
    fun `can deserialize actual Aidbox PatientsIdentifiers JSON`() {
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
        val deserializedPatientsIdentifiers = objectMapper.readValue<PatientsIdentifiers>(actualJson)

        assertEquals(deserializedPatientsIdentifiers.patientList?.size, 2)
    }

    @Test
    fun `getPatientsByTenant - success`() {
        val response = GraphQLResponse(data = mockPatientsIdentifiers)
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientsByTenant(tenantMnemonic)
        assertEquals(listOf("123", "456"), actual.keys.toList())
    }

    @Test
    fun `getPatientsByTenant - no data`() {
        val response = GraphQLResponse<PatientsIdentifiers>(data = null)
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientsByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }

    @Test
    fun `getPatientsByTenant - null patient list data`() {
        val response = GraphQLResponse(data = PatientsIdentifiers(null))
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientsByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }

    @Test
    fun `getPatientsByTenant - empty data`() {
        val response = GraphQLResponse(data = PatientsIdentifiers(listOf()))
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } throws Exception()

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
        val mrnSystemValue3 = SystemValue(system = "mrnSystem", value = "01113")
        val mrnIdentifier3 = Identifier(system = Uri("mrnSystem"), value = "01113".asFHIR())
        val fhirIdentifier3 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId3".asFHIR())

        val mockPatientIdentifiers3 = AidboxIdentifiers(
            udpId = "udpId3",
            identifiers = listOf(
                tenantIdentifier,
                mrnIdentifier3,
                fhirIdentifier3
            )
        )
        val response1 = GraphQLResponse(
            data = PatientsIdentifiers(listOf(mockPatientIdentifiers1, mockPatientIdentifiers2))
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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse1.body() } returns response1

        val response2 = GraphQLResponse(
            data = PatientsIdentifiers(listOf(mockPatientIdentifiers3))
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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse2.body() } returns response2

        val actualMap =
            patientService.getPatientFHIRIds(
                tenantMnemonic,
                mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2, "3" to mrnSystemValue3)
            )

        assertEquals(3, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
        assertEquals("fhirId2", actualMap["2"])
        assertEquals("fhirId3", actualMap["3"])
    }

    @Test
    fun `getPatient - success`() {
        val httpMock = mockk<HttpResponse>()
        val patientMock = mockk<Patient>()
        every { patientMock.identifier } returns listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                value = tenantMnemonic.asFHIR()
            )
        )
        coEvery { httpMock.body<Patient>() } returns patientMock
        coEvery { aidboxClient.getResource("Patient", "123") } returns httpMock
        val actual = patientService.getPatientByUDPId(tenantMnemonic, "123")
        assertEquals(patientMock, actual)
    }

    @Test
    fun `getPatient - fails with non-matching tenant`() {
        val httpMock = mockk<HttpResponse>()
        val patientMock = mockk<Patient>()
        every { patientMock.identifier } returns listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                value = tenantMnemonic.asFHIR()
            )
        )
        coEvery { httpMock.body<Patient>() } returns patientMock
        coEvery { aidboxClient.getResource("Patient", "123") } returns httpMock
        assertThrows<InvalidTenantAccessException> { patientService.getPatientByUDPId("newTenant", "123") }
    }

    @Test
    fun `graphql errors are properly parsed for getPatientsByTenant`() {
        val mockHttpResponse = mockk<HttpResponse>()
        val data = this::class.java.getResource("/FailedPatientQuery.json")!!.readText()
        val response = objectMapper.readValue<GraphQLResponse<PatientsIdentifiers>>(data)

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = patientService.getPatientsByTenant(tenantMnemonic)
        assertEquals(emptyMap<String, List<Identifier>>(), actual)
    }

    @Test
    fun `graphql errors are properly parsed for getPatientFHIRIds`() {
        val mockHttpResponse = mockk<HttpResponse>()
        val data = this::class.java.getResource("/FailedPatientQuery.json")!!.readText()
        val response2 = objectMapper.readValue<GraphQLResponse<PatientsIdentifiers>>(data)
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response2

        val mrnSystemValue3 = SystemValue(system = "mrnSystem", value = "01113")

        val response1 = GraphQLResponse(
            data = PatientsIdentifiers(listOf(mockPatientIdentifiers1, mockPatientIdentifiers2))
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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse1.body() } returns response1

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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse2.body() } returns response2

        val actualMap =
            patientService.getPatientFHIRIds(
                tenantMnemonic,
                mapOf("1" to mrnSystemValue1, "2" to mrnSystemValue2, "3" to mrnSystemValue3)
            )

        assertEquals(2, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
        assertEquals("fhirId2", actualMap["2"])
    }

    @Test
    fun `getFHIRIdsForTenant - success`() {
        val response = GraphQLResponse(data = mockPatientsIdentifiers)
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

        val expected = listOf("123", "456")
        val actual = patientService.getPatientFHIRIdsByTenant(tenantMnemonic)

        assertEquals(expected, actual)
    }

    @Test
    fun `getFHIRIdsForTenant - no data`() {
        val response = GraphQLResponse(data = PatientsIdentifiers(listOf()))
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = patientListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } returns response

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
        coEvery<GraphQLResponse<PatientsIdentifiers>> { mockHttpResponse.body() } throws Exception()

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
