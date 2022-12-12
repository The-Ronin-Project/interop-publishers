package com.projectronin.interop.aidbox

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.AidboxIdentifiers
import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.AIDBOX_LOCATION_FHIR_IDS_QUERY
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
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

    private val tenantQueryString = "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic"
    private val tenantIdentifier =
        Identifier(system = CodeSystem.RONIN_TENANT.uri, value = tenantMnemonic.asFHIR())

    private val locationSystemValue1 = SystemValue(system = CodeSystem.NPI.uri.value!!, value = location1)
    private val locationIdentifier1 = Identifier(system = CodeSystem.NPI.uri, value = location1.asFHIR())
    private val locationFhirIdentifier1 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId1".asFHIR())

    private val locationSystemValue2 = SystemValue(system = CodeSystem.NPI.uri.value!!, value = location2)
    private val locationIdentifier2 = Identifier(system = CodeSystem.NPI.uri, value = location2.asFHIR())
    private val locationFhirIdentifier = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId2".asFHIR())

    private val mockLocationIdentifiers1 = AidboxIdentifiers(
        udpId = "udpId1",
        identifiers = listOf(
            tenantIdentifier,
            locationIdentifier1,
            locationFhirIdentifier1
        )
    )
    private val mockLocationIdentifiers2 = AidboxIdentifiers(
        udpId = "udpId2",
        identifiers = listOf(
            tenantIdentifier,
            locationIdentifier2,
            locationFhirIdentifier
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
            data = LocationsIdentifiers(listOf(mockLocationIdentifiers1, mockLocationIdentifiers2))
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns response

        var actualMap = locationService.getAllLocationIdentifiers(
            tenantMnemonic,
            listOf(locationSystemValue1, locationSystemValue2)
        )

        assertEquals(2, actualMap.size)

        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns GraphQLResponse(
            data = LocationsIdentifiers(emptyList())
        )
        actualMap = locationService.getAllLocationIdentifiers(
            tenantMnemonic,
            listOf(locationSystemValue1, locationSystemValue2)
        )
        assertEquals(0, actualMap.size)

        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns GraphQLResponse(
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
            data = LocationsIdentifiers(listOf(mockLocationIdentifiers1, mockLocationIdentifiers2))
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(2, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
        assertEquals("fhirId2", actualMap["2"])
    }

    @Test
    fun `getFHIRIDs returns some locations`() {
        val response = GraphQLResponse(
            data = LocationsIdentifiers(listOf(mockLocationIdentifiers1))
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(1, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
    }

    @Test
    fun `getFHIRIDs with empty data returns no locations`() {
        val response = GraphQLResponse(
            data = LocationsIdentifiers(emptyList())
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs with null data returns no locations`() {
        val response = GraphQLResponse(
            data = LocationsIdentifiers(null)
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns response

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2)
        )

        assertEquals(0, actualMap.size)
    }

    @Test
    fun `getFHIRIDs returns GraphQL errors`() {
        val response = GraphQLResponse<LocationsIdentifiers>(
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse.body() } returns response

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
            JacksonManager.objectMapper.readValue<LocationsIdentifiers>(actualJson)

        assertEquals(deserializedLimitedLocation.locationList?.size, 2)
    }

    @Test
    fun `getFHIRIDs returns all batched locations`() {
        val locationSystemValue3 = SystemValue(system = CodeSystem.NPI.uri.value!!, value = "01113")
        val locationIdentifier3 = Identifier(system = CodeSystem.NPI.uri, value = "01113".asFHIR())
        val locationFhirIdentifier3 = Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = "fhirId3".asFHIR())

        val mockLocationIdentifiers3 = AidboxIdentifiers(
            udpId = "udpId",
            identifiers = listOf(
                tenantIdentifier,
                locationIdentifier3,
                locationFhirIdentifier3
            )
        )

        val response1 = GraphQLResponse(
            data = LocationsIdentifiers(listOf(mockLocationIdentifiers1, mockLocationIdentifiers2))
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse1.body() } returns response1

        val response2 = GraphQLResponse(
            data = LocationsIdentifiers(listOf(mockLocationIdentifiers3))
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
        coEvery<GraphQLResponse<LocationsIdentifiers>> { mockHttpResponse2.body() } returns response2

        val actualMap = locationService.getLocationFHIRIds(
            tenantMnemonic,
            mapOf("1" to locationSystemValue1, "2" to locationSystemValue2, "3" to locationSystemValue3)
        )

        assertEquals(3, actualMap.size)
        assertEquals("fhirId1", actualMap["1"])
        assertEquals("fhirId2", actualMap["2"])
        assertEquals("fhirId3", actualMap["3"])
    }
}
