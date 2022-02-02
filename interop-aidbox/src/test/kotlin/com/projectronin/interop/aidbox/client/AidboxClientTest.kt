package com.projectronin.interop.aidbox.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.projectronin.interop.aidbox.auth.AuthenticationBroker
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.RedirectResponseException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AidboxClientTest {
    private val urlRest = "http://localhost/8888"
    private val urlBatchUpsert = "$urlRest/"
    private val collection = listOf(
        OncologyPractitioner(
            id = Id("cmjones"),
            identifier = listOf(
                Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.RONIN_TENANT, value = "third")
            ),
            meta = null,
            implicitRules = null,
            language = null,
            text = null,
            contained = listOf(),
            extension = listOf(),
            modifierExtension = listOf(),
            active = null,
            name = listOf(
                HumanName(family = "Jones", given = listOf("Cordelia", "May"))
            ),
            telecom = listOf(),
            address = listOf(),
            gender = null,
            birthDate = null,
            photo = listOf(),
            qualification = listOf(),
            communication = listOf()
        ),
        OncologyPractitioner(
            id = Id("rallyr"),
            meta = null,
            implicitRules = null,
            language = null,
            text = null,
            contained = listOf(),
            extension = listOf(),
            modifierExtension = listOf(),
            identifier = listOf(
                Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.RONIN_TENANT, value = "second")
            ),
            active = null,
            name = listOf(
                HumanName(
                    family = "Llyr", given = listOf("Regan", "Anne")
                )
            ),
            telecom = listOf(),
            address = listOf(),
            gender = null,
            birthDate = null,
            photo = listOf(),
            qualification = listOf(),
            communication = listOf()
        )
    )
    private val collectionString = objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS).writeValueAsString(collection)

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 200`() {
        val expectedResponseStatus = HttpStatusCode.OK
        val aidboxClient =
            createClient(collectionString, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(collection)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 1xx`() {
        val expectedResponseStatus = HttpStatusCode.Continue
        val aidboxClient =
            createClient(collectionString, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(collection)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 2xx`() {
        val expectedResponseStatus = HttpStatusCode.Accepted
        val aidboxClient = createClient(collectionString, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(collection)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 3xx exception`() {
        val expectedResponseStatus = HttpStatusCode.TemporaryRedirect
        val aidboxClient = createClient(collectionString, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(RedirectResponseException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(collection)
            }
        }
        assertEquals(exception.response.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 4xx exception`() {
        val expectedResponseStatus = HttpStatusCode.Unauthorized
        val aidboxClient = createClient(collectionString, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(ClientRequestException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(collection)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    @Test
    fun `aidbox batch upsert of 2 Practitioner resources, response 5xx exception`() {
        val expectedResponseStatus = HttpStatusCode.ServiceUnavailable
        val exception = assertThrows(ServerResponseException::class.java) {
            val aidboxClient = createClient(collectionString, urlBatchUpsert, responseStatus = expectedResponseStatus)
            runBlocking {
                aidboxClient.batchUpsert(collection)
            }
        }
        assertEquals(exception.response.status, expectedResponseStatus)
    }

    private fun createClient(
        expectedBody: String,
        expectedUrl: String,
        baseUrl: String = urlRest,
        responseStatus: HttpStatusCode = HttpStatusCode.OK
    ): AidboxClient {
        val authenticationBroker = mockk<AuthenticationBroker> {
            every { getAuthentication() } returns mockk {
                every { tokenType } returns "Bearer"
                every { accessToken } returns "Auth-String"
            }
        }

        val mockEngine = MockEngine { request ->
            assertEquals(expectedUrl, request.url.toString())
            assertEquals(expectedBody, String(request.body.toByteArray())) // see if this is a JSON string/stream
            assertEquals("Bearer Auth-String", request.headers["Authorization"])
            respond(
                content = "",
                status = responseStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(JsonFeature) {
                serializer = JacksonSerializer() {
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                }
            }
        }

        return AidboxClient(httpClient, baseUrl, authenticationBroker)
    }
}
