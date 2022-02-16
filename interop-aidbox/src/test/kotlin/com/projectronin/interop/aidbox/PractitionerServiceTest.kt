package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.GraphQLError
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import io.ktor.client.call.receive
import io.ktor.client.features.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PractitionerServiceTest {
    private val mockPractitioner1 = LimitedPractitioner(
        practitioner = LimitedPractitionerIDs(
            identifier = listOf(
                Identifier(
                    value = "tenant-id", type = CodeableConcepts.RONIN_TENANT
                ),
                Identifier(
                    value = "22221", type = CodeableConcepts.SER
                ),
                Identifier(
                    value = "9988776655"
                )
            )
        )
    )
    private val mockPractitioner2 = LimitedPractitioner(
        practitioner = LimitedPractitionerIDs(
            identifier = listOf(
                Identifier(
                    value = "mdaoc", type = CodeableConcepts.RONIN_TENANT
                ),
                Identifier(
                    value = "22222", type = CodeableConcepts.SER
                ),
                Identifier(
                    value = "2281376654"
                )
            )
        )
    )
    private val mockPractitionerList = PractitionerList(
        practitionerList = listOf(
            PartialPractitioner(
                identifier = listOf(
                    Identifier(
                        value = "mdaoc", type = CodeableConcepts.RONIN_TENANT
                    ),
                    Identifier(
                        value = "22221", type = CodeableConcepts.SER
                    ),
                    Identifier(
                        value = "9988776655"
                    )
                ),
                id = Id("123"),
            ),
            PartialPractitioner(
                identifier = listOf(
                    Identifier(
                        value = "mdaoc", type = CodeableConcepts.RONIN_TENANT
                    ),
                    Identifier(
                        value = "22222", type = CodeableConcepts.SER
                    ),
                    Identifier(
                        value = "2281376654"
                    )
                ),
                id = Id("456"),
            )
        )
    )
    private val aidboxClient = mockk<AidboxClient>()
    private val practitionerService = PractitionerService(aidboxClient)
    private val query = javaClass.getResource("/graphql/AidboxLimitedPractitionerIDsQuery.graphql")!!.readText()
    private val practitionerListQuery = javaClass.getResource("/graphql/PractitionerListQuery.graphql")!!.readText()

    @Test
    fun `getPractitionerIdentifiers - happy`() {
        val response = GraphQLResponse(data = mockPractitioner1)
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("id" to fhirID)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPractitioner>> { mockHttpResponse.receive() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(fhirID)

        val expected = response.data?.practitioner?.identifier
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - no data`() {
        val response = GraphQLResponse(data = LimitedPractitioner(practitioner = LimitedPractitionerIDs(identifier = listOf())))
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("id" to fhirID)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPractitioner>> { mockHttpResponse.receive() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(fhirID)

        val expected = response.data?.practitioner?.identifier
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
                parameters = mapOf("id" to fhirID)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPractitioner>> { mockHttpResponse.receive() } returns response

        val actual = practitionerService.getPractitionerIdentifiers(fhirID)

        val expected = listOf<Identifier>() // return empty list on graphQL error
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - response exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.receive<String>() } returns "Unauthorized"
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("id" to fhirID)
            )
        } throws ResponseException(mockHttpResponse, "Response Text")

        val actual = practitionerService.getPractitionerIdentifiers(fhirID)

        val expected = listOf<Identifier>() // return empty list on graphQL error
        assertEquals(actual, expected)
    }

    @Test
    fun `getPractitionerIdentifiers - other exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.receive<String>() } returns "Unauthorized"
        val fhirID = "roninMDAPractitioner01Test"
        coEvery { aidboxClient.queryGraphQL(query = query, parameters = mapOf("id" to fhirID)) } throws Exception()

        assertThrows<Exception> { practitionerService.getPractitionerIdentifiers(fhirID) }
    }

    @Test
    fun getSpecificPractitionerIdentifier() {
        val response = GraphQLResponse(data = mockPractitioner1)
        val mockHttpResponse = mockk<HttpResponse>()
        val fhirID = "roninMDAPractitioner01Test"
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("id" to fhirID)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPractitioner>> { mockHttpResponse.receive() } returns response

        val actual = practitionerService.getSpecificPractitionerIdentifier(fhirID, CodeableConcepts.SER)

        val expected = Identifier(
            value = "22221", type = CodeableConcepts.SER
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
                parameters = mapOf("id" to fhirID)
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<LimitedPractitioner>> { mockHttpResponse.receive() } returns response

        val actual = practitionerService.getSpecificPractitionerIdentifier(fhirID, CodeableConcepts.MRN)

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
                parameters = mapOf("id" to fhirID1)
            )
        } returns mockHttpResponse1
        coEvery<GraphQLResponse<LimitedPractitioner>> { mockHttpResponse1.receive() } returns response1
        coEvery {
            aidboxClient.queryGraphQL(
                query = query,
                parameters = mapOf("id" to fhirID2)
            )
        } returns mockHttpResponse2
        coEvery<GraphQLResponse<LimitedPractitioner>> { mockHttpResponse2.receive() } returns response2

        val actual = practitionerService.getPractitionersIdentifiers(listOf(fhirID1, fhirID2))

        val expected1 = response1.data?.practitioner?.identifier
        val expected2 = response2.data?.practitioner?.identifier
        assertEquals(actual[fhirID1], expected1)
        assertEquals(actual[fhirID2], expected2)
    }

    @Test
    fun getPractitionersByTenant() {
        val response = GraphQLResponse(data = mockPractitionerList)
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionerList>> { mockHttpResponse.receive() } returns response

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic)
        val fhirIds = response.data?.practitionerList?.map {
            it.id.value
        }
        assertEquals(fhirIds, actual.keys.toList())
    }

    @Test
    fun `getPractitionersByTenant - no data`() {
        val response = GraphQLResponse(data = PractitionerList(listOf()))
        val mockHttpResponse = mockk<HttpResponse>()

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionerList>> { mockHttpResponse.receive() } returns response

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }

    @Test
    fun `getPractitionersByTenant - exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.receive<String>() } returns "Unauthorized"

        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } returns mockHttpResponse
        coEvery<GraphQLResponse<PractitionerList>> { mockHttpResponse.receive() } throws Exception()

        assertThrows<Exception> { practitionerService.getPractitionersByTenant(tenantMnemonic) }
    }

    @Test
    fun `getPractitionersByTenant - response exception`() {
        val mockHttpResponse = mockk<HttpResponse>()
        every { mockHttpResponse.status } returns HttpStatusCode(401, "Unauthorized")
        coEvery { mockHttpResponse.receive<String>() } returns "Unauthorized"
        val tenantMnemonic = "tenant-id"
        coEvery {
            aidboxClient.queryGraphQL(
                query = practitionerListQuery,
                parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
            )
        } throws ResponseException(mockHttpResponse, "Response text")

        val actual = practitionerService.getPractitionersByTenant(tenantMnemonic)

        assertEquals(mutableMapOf<String, List<Identifier>>(), actual)
    }
}
