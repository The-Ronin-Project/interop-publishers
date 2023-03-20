package com.projectronin.interop.aidbox

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.exception.InvalidTenantAccessException
import com.projectronin.interop.aidbox.model.AidboxIdentifiers
import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.AIDBOX_LIMITED_PRACTITIONER_IDS_QUERY
import com.projectronin.interop.aidbox.utils.AIDBOX_PRACTITIONER_FHIR_IDS_QUERY
import com.projectronin.interop.aidbox.utils.AIDBOX_PRACTITIONER_LIST_QUERY
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Practitioner
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

class PractitionerServiceTest {
    private val identifiers1 = listOf(
        Identifier(value = "tenant-id".asFHIR(), type = CodeableConcept(text = "tenant".asFHIR())),
        Identifier(value = "22221".asFHIR(), type = CodeableConcept(text = "ser".asFHIR())),
        Identifier(value = "9988776655".asFHIR()),
        Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId1".asFHIR())
    )
    private val mockPractitioner1 = PractitionersIdentifiers(
        practitionerList = listOf(
            AidboxIdentifiers(
                identifiers = identifiers1,
                udpId = ""
            )
        )
    )

    private val identifiers2 = listOf(
        Identifier(value = "mdaoc".asFHIR(), type = CodeableConcept(text = "tenant".asFHIR())),
        Identifier(value = "22222".asFHIR(), type = CodeableConcept(text = "ser".asFHIR())),
        Identifier(value = "2281376654".asFHIR()),
        Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId2".asFHIR())
    )
    private val mockPractitioner2 = PractitionersIdentifiers(
        practitionerList = listOf(
            AidboxIdentifiers(
                identifiers = identifiers2,
                udpId = ""
            )
        )
    )
    private val mockPractitionersIdentifiers = PractitionersIdentifiers(
        practitionerList = listOf(
            AidboxIdentifiers(
                identifiers = identifiers1,
                udpId = "123"
            ),
            AidboxIdentifiers(
                identifiers = identifiers2,
                udpId = "456"
            )
        )
    )
    private val aidboxClient = mockk<AidboxClient>()
    private val practitionerService = PractitionerService(aidboxClient, 2)
    private val query = AIDBOX_LIMITED_PRACTITIONER_IDS_QUERY
    private val practitionerListQuery = AIDBOX_PRACTITIONER_LIST_QUERY
    private val queryFHIR = AIDBOX_PRACTITIONER_FHIR_IDS_QUERY

    private val tenantMnemonic = "mdaoc"
    private val practitioner1 = "01111"
    private val practitioner2 = "01112"

    private val tenantQueryString = "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic"
    private val tenantIdentifier =
        Identifier(system = CodeSystem.RONIN_TENANT.uri, value = tenantMnemonic.asFHIR())

    private val practitionerSystemValue1 = SystemValue(system = CodeSystem.NPI.uri.value!!, value = practitioner1)
    private val practitionerIdentifier1 = Identifier(system = CodeSystem.NPI.uri, value = practitioner1.asFHIR())
    private val practitionerFhirId1 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId1".asFHIR())

    private val practitionerSystemValue2 = SystemValue(system = CodeSystem.NPI.uri.value!!, value = practitioner2)
    private val practitionerIdentifier2 = Identifier(system = CodeSystem.NPI.uri, value = practitioner2.asFHIR())
    private val practitionerFhirId2 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId2".asFHIR())

    private val mockPractitionerIdentifiers1 = AidboxIdentifiers(
        udpId = "udpID1",
        identifiers = listOf(
            tenantIdentifier,
            practitionerIdentifier1,
            practitionerFhirId1
        )
    )
    private val mockPractitionerIdentifiers2 = AidboxIdentifiers(
        udpId = "udpID2",
        identifiers = listOf(
            tenantIdentifier,
            practitionerIdentifier2,
            practitionerFhirId2
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
    fun `getPractitionerIdentifiers - happy`() {
        val response = GraphQLResponse(data = mockPractitioner1)
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID)

        val expected = response.data?.practitionerList?.firstOrNull()?.identifiers
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - empty identifier list`() {
        val response =
            GraphQLResponse(
                data = PractitionersIdentifiers(
                    practitionerList = listOf(
                        AidboxIdentifiers(
                            identifiers = listOf(),
                            udpId = ""
                        )
                    )
                )
            )
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID)

        val expected = listOf<Identifier>()
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - empty practitioner list`() {
        val response =
            GraphQLResponse(
                data = PractitionersIdentifiers(
                    practitionerList = listOf()
                )
            )
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID)

        val expected = listOf<Identifier>()
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - no data`() {
        val response =
            GraphQLResponse<PractitionersIdentifiers>(
                data = null
            )
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID)

        val expected = listOf<Identifier>()
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - different tenant`() {
        val response =
            GraphQLResponse(
                data = PractitionersIdentifiers(
                    practitionerList = listOf(
                        AidboxIdentifiers(
                            identifiers = listOf(),
                            udpId = ""
                        )
                    )
                )
            )
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to "${CodeSystem.RONIN_TENANT.uri.value}|wrongTenant")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionerIdentifiers("wrongTenant", fhirID)

        val expected = listOf<Identifier>()
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - errors`() {
        val response = GraphQLResponse(data = mockPractitioner1, errors = listOf(GraphQLError("Error occurred")))
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID)

        val expected = listOf<Identifier>() // return empty list on graphQL error
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - response exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to tenantQueryString)
            )
        } throws ClientFailureException(HttpStatusCode.ServiceUnavailable, "")

        val actual = practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID)

        val expected = listOf<Identifier>() // return empty list on graphQL error
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - other exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("id" to fhirID, "tenant" to tenantQueryString)
            )
        } throws Exception()

        assertThrows<Exception> { practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID) }
    }

    @Test
    fun getSpecificPractitionerIdentifier() {
        val response = GraphQLResponse(data = mockPractitioner1)
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("tenant" to tenantQueryString, "fhirId" to "http://projectronin.com/id/fhir|$fhirID")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual =
            practitionerService.getSpecificPractitionerIdentifier(tenantMnemonic, fhirID, CodeableConcept(text = "ser".asFHIR()))

        val expected = Identifier(
            value = "22221".asFHIR(),
            type = CodeableConcept(text = "ser".asFHIR())
        )
        assertEquals(actual, expected)
    }

    @Test
    fun `getSpecificPractitionerIdentifier - not found`() {
        val response = GraphQLResponse(data = mockPractitioner1)
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual =
            practitionerService.getSpecificPractitionerIdentifier(tenantMnemonic, fhirID, CodeableConcept(text = "mrn".asFHIR()))

        val expected = null
        assertEquals(actual, expected)
    }

    @Test
    fun getPractitionersIdentifiers() {
        val response1 = GraphQLResponse(data = mockPractitioner1)
        val response2 = GraphQLResponse(data = mockPractitioner2)
        val mockHttpResponse1 = mockk<HttpResponse>()
        val mockHttpResponse2 = mockk<HttpResponse>()

        val fhirID1 = "roninMDAPractitioner01Test"
        val fhirID2 = "roninMDAPractitioner02Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID1", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse1
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse1.body() } returns response1
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("fhirId" to "http://projectronin.com/id/fhir|$fhirID2", "tenant" to tenantQueryString)
            )
        } returns mockHttpResponse2
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse2.body() } returns response2

        val actual = practitionerService.getPractitionersIdentifiers(tenantMnemonic, listOf(fhirID1, fhirID2))

        val expected1 = response1.data?.practitionerList?.firstOrNull()?.identifiers
        val expected2 = response2.data?.practitionerList?.firstOrNull()?.identifiers
        assertEquals(actual[fhirID1], expected1)
        assertEquals(actual[fhirID2], expected2)
    }

    @Test
    fun `getFHIRIDs returns all practitioners`() {
        val response = GraphQLResponse(
            data = PractitionersIdentifiers(listOf(mockPractitionerIdentifiers1, mockPractitionerIdentifiers2))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = practitionerService.getPractitionerFHIRIds(
            tenantMnemonic,
            mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2)
        )

        assertEquals(2, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
        assertEquals("fhirId2", actualMap["2"])
    }

    @Test
    fun `getFHIRIDs returns some practitioners`() {
        val response = GraphQLResponse(
            data = PractitionersIdentifiers(listOf(mockPractitionerIdentifiers1))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = practitionerService.getPractitionerFHIRIds(
            tenantMnemonic,
            mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2)
        )

        assertEquals(1, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
    }

    @Test
    fun `getFHIRIDs returns no practitioners`() {
        val response = GraphQLResponse(
            data = PractitionersIdentifiers(listOf())
        )
        val mockHttpResponse = mockk<HttpResponse>()

        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = practitionerService.getPractitionerFHIRIds(
            tenantMnemonic,
            mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2)
        )

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs returns GraphQL errors`() {
        val response = GraphQLResponse<PractitionersIdentifiers>(
            errors = listOf(GraphQLError("GraphQL Error"))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = practitionerService.getPractitionerFHIRIds(
            tenantMnemonic,
            mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2)
        )

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs throws a ResponseException`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } throws ClientFailureException(HttpStatusCode.ServiceUnavailable, "")

        val actualMap = practitionerService.getPractitionerFHIRIds(
            tenantMnemonic,
            mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2)
        )

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs throws a non-response exception`() {
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } throws Exception("not a ResponseException")

        val exception = assertThrows<Exception> {
            practitionerService.getPractitionerFHIRIds(
                tenantMnemonic,
                mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2)
            )
        }
        assertEquals(exception.message, "not a ResponseException")
    }

    @Test
    fun `can deserialize actual Aidbox PractitionersIdentifiersFHIRIdentifiers JSON`() {
        val actualJson = """
        {
          "id": "mdaoc-e3Dt5qIBhMpHNwBK2q370pg3",
          "identifier": [
            {
              "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780",
              "value": "  30777"
            },
            {
              "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780",
              "value": "30777"
            },
            {
              "system": "POTFID",
              "value": "30777"
            },
            {
              "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
              "value": "   30777"
            },
            {
              "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
              "value": "30777"
            },
            {
              "system": "http://projectronin.com/id/tenantId",
              "value": "mdaoc"
            }
          ]
        }
        """.trimIndent()
        val deserializedPractitionersIdentifiersFHIRIdentifiers =
            JacksonManager.objectMapper.readValue<AidboxIdentifiers>(actualJson)

        assertEquals(deserializedPractitionersIdentifiersFHIRIdentifiers.udpId, "mdaoc-e3Dt5qIBhMpHNwBK2q370pg3")
        assertEquals(deserializedPractitionersIdentifiersFHIRIdentifiers.identifiers.size, 6)

        val identifier1 = deserializedPractitionersIdentifiersFHIRIdentifiers.identifiers[1]
        assertEquals(identifier1.system?.value, "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780")
        assertEquals(identifier1.value?.value, "30777")

        val identifier2 = deserializedPractitionersIdentifiersFHIRIdentifiers.identifiers[2]
        assertEquals(identifier2.system?.value, "POTFID")
        assertEquals(identifier2.value?.value, "30777")

        val identifier5 = deserializedPractitionersIdentifiersFHIRIdentifiers.identifiers[5]
        assertEquals(identifier5.system?.value, CodeSystem.RONIN_TENANT.uri.value)
        assertEquals(identifier5.value?.value, "mdaoc")
    }

    @Test
    fun `can deserialize actual Aidbox PractitionersIdentifiers JSON`() {
        val actualJson = """
          {
            "PractitionerList": [
              {
                "id": "mdaoc-e2gxs4Fn7l-TbzLO17uxuHw3",
                "identifier": [
                  {
                    "system": "POTFID",
                    "value": "E4881"
                  },
                  {
                    "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
                    "value": "   E4881"
                  },
                  {
                    "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
                    "value": "E4881"
                  },
                  {
                    "system": "http://projectronin.com/id/tenantId",
                    "value": "mdaoc"
                  }
                ]
              },
              {
                "id": "mdaoc-e3Dt5qIBhMpHNwBK2q370pg3",
                "identifier": [
                  {
                    "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780",
                    "value": "  30777"
                  },
                  {
                    "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780",
                    "value": "30777"
                  },
                  {
                    "system": "POTFID",
                    "value": "30777"
                  },
                  {
                    "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
                    "value": "   30777"
                  },
                  {
                    "system": "urn:oid:1.2.840.114350.1.13.0.1.7.2.836982",
                    "value": "30777"
                  },
                  {
                    "system": "http://projectronin.com/id/tenantId",
                    "value": "mdaoc"
                  }
                ]
              }
            ]
          }
        """.trimIndent()
        val deserializedPractitionersIdentifiers =
            JacksonManager.objectMapper.readValue<PractitionersIdentifiers>(actualJson)

        assertEquals(deserializedPractitionersIdentifiers.practitionerList?.size, 2)
    }

    @Test
    fun getPractitionersByTenant() {
        val response = GraphQLResponse(data = mockPractitionersIdentifiers)
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf(
                    "identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic",
                    "count" to "100",
                    "page" to "1"
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic)
        assertEquals(listOf("fhirId1", "fhirId2"), actual.keys.toList())
    }

    @Test
    fun `getPractitionersByTenant - no data`() {
        val response = GraphQLResponse(data = PractitionersIdentifiers(listOf()))
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf(
                    "identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic",
                    "count" to "100",
                    "page" to "1"
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }

    @Test
    fun `getPractitionersByTenant - multiple pages`() {
        val identifiers3 = listOf(
            Identifier(value = "tenant-id".asFHIR(), type = CodeableConcept(text = "tenant".asFHIR())),
            Identifier(value = "22221".asFHIR(), type = CodeableConcept(text = "ser".asFHIR())),
            Identifier(value = "9988776655".asFHIR()),
            Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId3".asFHIR())
        )
        val practitioner1 = mockk<AidboxIdentifiers>() {
            every { udpId } returns "udpId1"
            every { identifiers } returns identifiers1
        }
        val practitioner2 = mockk<AidboxIdentifiers>() {
            every { udpId } returns "udpId2"
            every { identifiers } returns identifiers2
        }
        val practitioner3 = mockk<AidboxIdentifiers>() {
            every { udpId } returns "udpId3"
            every { identifiers } returns identifiers3
        }

        val response1 = GraphQLResponse(data = PractitionersIdentifiers(listOf(practitioner1, practitioner2)))
        val mockHttpResponse1 = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf(
                    "identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic",
                    "count" to "2",
                    "page" to "1"
                )
            )
        } returns mockHttpResponse1
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse1.body() } returns response1

        val response2 = GraphQLResponse(data = PractitionersIdentifiers(listOf(practitioner3)))
        val mockHttpResponse2 = mockk<HttpResponse>()

        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf(
                    "identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic",
                    "count" to "2",
                    "page" to "2"
                )
            )
        } returns mockHttpResponse2
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse2.body() } returns response2

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic, 2)

        assertEquals(3, actual.size)
        assertEquals(mapOf("fhirId1" to identifiers1, "fhirId2" to identifiers2, "fhirId3" to identifiers3), actual)
    }

    @Test
    fun `getPractitionersByTenant - exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } throws Exception()

        assertThrows<Exception> { practitionerService.getPractitionersByTenant(tenantMnemonic) }
    }

    @Test
    fun `getPractitionersByTenant - response exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.bodyAsText() } returns "Unauthorized"
        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf(
                    "identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic",
                    "count" to "100",
                    "page" to "1"
                )
            )
        } throws ClientFailureException(HttpStatusCode.ServiceUnavailable, "")

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }

    @Test
    fun `getFHIRIDs returns all batched practitioners`() {
        val practitionerSystemValue3 = SystemValue(system = CodeSystem.NPI.uri.value!!, value = "01113")
        val practitionerIdentifier3 = Identifier(system = CodeSystem.NPI.uri, value = "01113".asFHIR())
        val practitionerFhirId3 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId3".asFHIR())

        val mockPractitionerIdentifiers3 = AidboxIdentifiers(
            udpId = "udpId3",
            identifiers = listOf(
                tenantIdentifier,
                practitionerIdentifier3,
                practitionerFhirId3
            )
        )

        val response1 = GraphQLResponse(
            data = PractitionersIdentifiers(listOf(mockPractitionerIdentifiers1, mockPractitionerIdentifiers2))
        )
        val mockHttpResponse1 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse1
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse1.body() } returns response1

        val response2 = GraphQLResponse(
            data = PractitionersIdentifiers(listOf(mockPractitionerIdentifiers3))
        )
        val mockHttpResponse2 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue3
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse2
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse2.body() } returns response2

        val actualMap = practitionerService.getPractitionerFHIRIds(
            tenantMnemonic,
            mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2, "3" to practitionerSystemValue3)
        )

        assertEquals(3, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
        assertEquals("fhirId2", actualMap["2"])
        assertEquals("fhirId3", actualMap["3"])
    }

    @Test
    fun `get full practitioner test`() {
        val httpMock = mockk<HttpResponse>()
        val practitionerMock = mockk<Practitioner>()
        every { practitionerMock.identifier } returns listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                value = tenantMnemonic.asFHIR()
            )
        )
        coEvery { httpMock.body<Practitioner>() } returns practitionerMock
        coEvery { aidboxClient.getResource("Practitioner", "123") } returns httpMock
        val actual = practitionerService.getPractitionerByUDPId(tenantMnemonic, "123")
        assertEquals(practitionerMock, actual)
    }

    @Test
    fun `get full practitioner fails with non-matching tenant`() {
        val httpMock = mockk<HttpResponse>()
        val practitionerMock = mockk<Practitioner>()
        every { practitionerMock.identifier } returns listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                value = tenantMnemonic.asFHIR()
            )
        )
        coEvery { httpMock.body<Practitioner>() } returns practitionerMock
        coEvery { aidboxClient.getResource("Practitioner", "123") } returns httpMock
        assertThrows<InvalidTenantAccessException> { practitionerService.getPractitionerByUDPId("newTenant", "123") }
    }

    @Test
    fun `graphql errors are properly parsed for getPractitionersByTenant`() {
        val mockHttpResponse = mockk<HttpResponse>()
        val data = this::class.java.getResource("/FailedPractitionerQuery.json")!!.readText()
        val response = JacksonManager.objectMapper.readValue<GraphQLResponse<PractitionersIdentifiers>>(data)

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf(
                    "identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic",
                    "count" to "100",
                    "page" to "1"
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic)
        assertEquals(emptyMap<String, List<Identifier>>(), actual)
    }

    @Test
    fun `graphql errors are properly parsed for getPractitionerIdentifiers`() {
        val mockHttpResponse = mockk<HttpResponse>()
        val data = this::class.java.getResource("/FailedPractitionerQuery.json")!!.readText()
        val response = JacksonManager.objectMapper.readValue<GraphQLResponse<PractitionersIdentifiers>>(data)

        val tenantMnemonic = "tenant-id"
        val fhirID = "fhirId"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf(
                    "fhirId" to "http://projectronin.com/id/fhir|$fhirID",
                    "tenant" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic"
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(tenantMnemonic, fhirID)
        assertEquals(emptyList<Identifier>(), actual)
    }

    @Test
    fun `graphql errors are properly parsed for getPractitionerFHIRIds`() {
        val mockHttpResponse = mockk<HttpResponse>()
        val data = this::class.java.getResource("/FailedPractitionerQuery.json")!!.readText()
        val response = JacksonManager.objectMapper.readValue<GraphQLResponse<PractitionersIdentifiers>>(data)

        val tenantMnemonic = "mdaoc"

        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        practitionerSystemValue1,
                        practitionerSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionersIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = practitionerService.getPractitionerFHIRIds(
            tenantMnemonic,
            mapOf("1" to practitionerSystemValue1, "2" to practitionerSystemValue2)
        )
        assertEquals(emptyMap<String, String>(), actualMap)
    }
}
