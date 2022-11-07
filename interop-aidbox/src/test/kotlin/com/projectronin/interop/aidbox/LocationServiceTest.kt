package com.projectronin.interop.aidbox

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.AIDBOX_LOCATION_FHIR_IDS_QUERY
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
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

class LocationServiceTest {
    private val aidboxClient = mockk<AidboxClient>()
    private val locationService = LocationService(aidboxClient, 2)

    private val queryFHIR = AIDBOX_LOCATION_FHIR_IDS_QUERY

    private val tenantMnemonic = "ronin"
    private val location1 = "01111"
    private val location2 = "01112"

    private val tenantQueryString = "http://projectronin.com/id/tenantId|$tenantMnemonic"
    private val tenantIdentifier =
        Identifier(system = Uri("http://projectronin.com/id/tenantId"), value = tenantMnemonic)

    private val locationSystemValue1 = SystemValue(system = CodeSystem.NPI.uri.value, value = location1)
    private val locationIdentifier1 = Identifier(system = CodeSystem.NPI.uri, value = location1)

    private val locationSystemValue2 = SystemValue(system = CodeSystem.NPI.uri.value, value = location2)
    private val locationIdentifier2 = Identifier(system = CodeSystem.NPI.uri, value = location2)

    private val mockLocationIdentifiers1 = LimitedLocationFHIRIdentifiers(
        id = "roninLocation01Test",
        identifiers = listOf(
            tenantIdentifier,
            locationIdentifier1
        )
    )
    private val mockLocationIdentifiers2 = LimitedLocationFHIRIdentifiers(
        id = "roninLocation02Test",
        identifiers = listOf(
            tenantIdentifier,
            locationIdentifier2
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
    fun `identifier search returns all locations`() {
        val response = GraphQLResponse(
            data = LimitedLocationsFHIR(listOf(mockLocationIdentifiers1, mockLocationIdentifiers2))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns response

        var actualMap = locationService.getAllLocationIdentifiers(
            tenantMnemonic,
            listOf(locationSystemValue1, locationSystemValue2)
        )

        assertEquals(2, actualMap.size)

        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns GraphQLResponse(
            data = LimitedLocationsFHIR(emptyList())
        )
        actualMap = locationService.getAllLocationIdentifiers(
            tenantMnemonic,
            listOf(locationSystemValue1, locationSystemValue2)
        )
        assertEquals(0, actualMap.size)

        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns GraphQLResponse(
            data = null
        )
        actualMap = locationService.getAllLocationIdentifiers(
            tenantMnemonic,
            listOf(locationSystemValue1, locationSystemValue2)
        )
        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs returns all locations`() {
        val response = GraphQLResponse(
            data = LimitedLocationsFHIR(listOf(mockLocationIdentifiers1, mockLocationIdentifiers2))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(2, actualMap.size)
        assertEquals(mockLocationIdentifiers1.id, actualMap["1"])
        assertEquals(mockLocationIdentifiers2.id, actualMap["2"])
    }

    @Test
    fun `getFHIRIDs returns some locations`() {
        val response = GraphQLResponse(
            data = LimitedLocationsFHIR(listOf(mockLocationIdentifiers1))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(1, actualMap.size)
        assertEquals(mockLocationIdentifiers1.id, actualMap["1"])
    }

    @Test
    fun `getFHIRIDs with empty data returns no locations`() {
        val response = GraphQLResponse(
            data = LimitedLocationsFHIR(emptyList())
        )
        val mockHttpResponse = mockk<HttpResponse>()

        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs with null data returns no locations`() {
        val response = GraphQLResponse(
            data = LimitedLocationsFHIR(null)
        )
        val mockHttpResponse = mockk<HttpResponse>()

        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs returns GraphQL errors`() {
        val response = GraphQLResponse<LimitedLocationsFHIR>(
            errors = listOf(GraphQLError("GraphQL Error"))
        )
        val mockHttpResponse = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
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
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } throws ClientFailureException(HttpStatusCode.ServiceUnavailable, "")

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
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
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } throws Exception("not a ResponseException")

        val exception = assertThrows<Exception> {
            locationService.getLocationFHIRIds(
                tenantMnemonic,
                mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
            )
        }
        assertEquals(exception.message, "not a ResponseException")
    }

    @Test
    fun `can deserialize actual Aidbox LimitedLocationFHIRIdentifiers JSON`() {
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
        val deserializedLimitedLocationFHIRIdentifiers =
            JacksonManager.objectMapper.readValue<LimitedLocationFHIRIdentifiers>(actualJson)

        assertEquals(deserializedLimitedLocationFHIRIdentifiers.id, "mdaoc-e3Dt5qIBhMpHNwBK2q370pg3")
        assertEquals(deserializedLimitedLocationFHIRIdentifiers.identifiers.size, 6)

        val identifier1 = deserializedLimitedLocationFHIRIdentifiers.identifiers[1]
        assertEquals(identifier1.system?.value, "urn:oid:1.2.840.114350.1.13.0.1.7.2.697780")
        assertEquals(identifier1.value, "30777")

        val identifier2 = deserializedLimitedLocationFHIRIdentifiers.identifiers[2]
        assertEquals(identifier2.system?.value, "POTFID")
        assertEquals(identifier2.value, "30777")

        val identifier5 = deserializedLimitedLocationFHIRIdentifiers.identifiers[5]
        assertEquals(identifier5.system?.value, "http://projectronin.com/id/tenantId")
        assertEquals(identifier5.value, "mdaoc")
    }

    @Test
    fun `can deserialize actual Aidbox LimitedLocationsFHIR JSON`() {
        val actualJson = """
          {
            "LocationList": [
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
        val deserializedLimitedLocation =
            JacksonManager.objectMapper.readValue<LimitedLocationsFHIR>(actualJson)

        assertEquals(deserializedLimitedLocation.locationList?.size, 2)
    }

    @Test
    fun `getFHIRIDs returns all batched locations`() {
        val locationSystemValue3 = SystemValue(system = CodeSystem.NPI.uri.value, value = "01113")
        val locationIdentifier3 = Identifier(system = CodeSystem.NPI.uri, value = "01113")

        val mockLocationIdentifiers3 = LimitedLocationFHIRIdentifiers(
            id = "roninLocation01Test",
            identifiers = listOf(
                tenantIdentifier,
                locationIdentifier3
            )
        )

        val response1 = GraphQLResponse(
            data = LimitedLocationsFHIR(listOf(mockLocationIdentifiers1, mockLocationIdentifiers2))
        )
        val mockHttpResponse1 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue1,
                        locationSystemValue2
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse1
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse1.body() } returns response1

        val response2 = GraphQLResponse(
            data = LimitedLocationsFHIR(listOf(mockLocationIdentifiers3))
        )
        val mockHttpResponse2 = mockk<HttpResponse>()
        coEvery {
            aidboxClient.queryGraphQL(
                query = queryFHIR,
                parameters = mapOf(
                    "tenant" to tenantQueryString,
                    "identifiers" to listOf(
                        locationSystemValue3
                    ).joinToString(separator = ",") {
                        it.queryString
                    }
                )
            )
        } returns mockHttpResponse2
        coEvery<GraphQLResponse<LimitedLocationsFHIR>> { mockHttpResponse2.body() } returns response2

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2, "3" to locationSystemValue3)
        )

        assertEquals(3, actualMap.size)
        assertEquals(mockLocationIdentifiers1.id, actualMap["1"])
        assertEquals(mockLocationIdentifiers2.id, actualMap["2"])
        assertEquals(mockLocationIdentifiers3.id, actualMap["3"])
    }
}
