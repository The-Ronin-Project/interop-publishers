package com.projectronin.interop.aidbox.client

import com.projectronin.interop.aidbox.auth.AuthenticationBroker
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.RedirectResponseException
import io.ktor.client.features.ServerResponseException
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
    private val practitionerList = this::class.java.getResource("/json/AidboxPractitionerList.json")!!.readText()
    private val urlBatchUpsert = "$urlRest/"

    @Test
    fun `aidbox batch upsert of 3 Practitioner resources, response 200`() {
        val expectedResponseStatus = HttpStatusCode.OK
        val aidboxClient =
            createClient(practitionerList, urlBatchUpsert, practitionerList, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitionerList)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 3 Practitioner resources, response 1xx`() {
        val expectedResponseStatus = HttpStatusCode.Continue
        val aidboxClient = createClient(practitionerList, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitionerList)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 3 Practitioner resources, response 2xx`() {
        val expectedResponseStatus = HttpStatusCode.Accepted
        val aidboxClient = createClient(practitionerList, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitionerList)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 3 Practitioner resources, response 3xx exception`() {
        val expectedResponseStatus = HttpStatusCode.TemporaryRedirect
        val aidboxClient = createClient(practitionerList, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(RedirectResponseException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(practitionerList)
            }
        }
        assertEquals(exception.response.status, expectedResponseStatus)
    }

    @Test
    fun `aidbox batch upsert of 3 Practitioner resources, response 4xx exception`() {
        val expectedResponseStatus = HttpStatusCode.Unauthorized
        val aidboxClient = createClient(practitionerList, urlBatchUpsert, responseStatus = expectedResponseStatus)
        val exception = assertThrows(ClientRequestException::class.java) {
            runBlocking {
                aidboxClient.batchUpsert(practitionerList)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    @Test
    fun `aidbox batch upsert of 3 Practitioner resources, response 5xx exception`() {
        val expectedResponseStatus = HttpStatusCode.ServiceUnavailable
        val exception = assertThrows(ServerResponseException::class.java) {
            val aidboxClient = createClient(practitionerList, urlBatchUpsert, responseStatus = expectedResponseStatus)
            runBlocking {
                aidboxClient.batchUpsert(practitionerList)
            }
        }
        assertEquals(exception.response.status, expectedResponseStatus)
    }

    private fun createClient(
        expectedBody: String,
        expectedUrl: String,
        responseContent: String = "",
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
            assert(expectedUrl.startsWith(baseUrl, ignoreCase = true))
            assertEquals(expectedBody, String(request.body.toByteArray()))
            assertEquals("Bearer Auth-String", request.headers["Authorization"])
            respond(
                content = responseContent,
                status = responseStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
        }

        return AidboxClient(httpClient, baseUrl, authenticationBroker)
    }
}
