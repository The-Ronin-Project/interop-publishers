package com.projectronin.interop.aidbox.client

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
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AidboxClientTest {
    private val urlRest = "http://localhost/8888"
    private val authString = "Auth-String"
    private val practitionerList = this::class.java.getResource("/json/AidboxPractitionerList.json")!!.readText()

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 200 true`() {
        val urlPublish = "$urlRest/"
        val expectedBody = practitionerList
        val expectedResponseJSON = ""
        val expectedResponseStatus = HttpStatusCode.OK
        val aidboxClient = createClient(expectedBody, expectedResponseJSON, urlPublish, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitionerList)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 1xx false`() {
        val urlPublish = "$urlRest/"
        val expectedBody = practitionerList
        val expectedResponseJSON = ""
        val expectedResponseStatus = HttpStatusCode.Continue
        val aidboxClient = createClient(expectedBody, expectedResponseJSON, urlPublish, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitionerList)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 2xx false`() {
        val urlPublish = "$urlRest/"
        val expectedBody = practitionerList
        val expectedResponseJSON = ""
        val expectedResponseStatus = HttpStatusCode.Accepted
        val aidboxClient = createClient(expectedBody, expectedResponseJSON, urlPublish, responseStatus = expectedResponseStatus)
        val actualResponse: HttpResponse = runBlocking {
            aidboxClient.batchUpsert(practitionerList)
        }
        assertEquals(actualResponse.status, expectedResponseStatus)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 3xx exception`() {
        val expectedResponseStatus = HttpStatusCode.TemporaryRedirect
        val exception = assertThrows(RedirectResponseException::class.java) {
            val urlPublish = "$urlRest/"
            val expectedBody = practitionerList
            val expectedResponseJSON = ""
            val aidboxClient = createClient(expectedBody, expectedResponseJSON, urlPublish, responseStatus = expectedResponseStatus)
            runBlocking {
                aidboxClient.batchUpsert(practitionerList)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 4xx exception`() {
        val expectedResponseStatus = HttpStatusCode.Unauthorized
        val exception = assertThrows(ClientRequestException::class.java) {
            val urlPublish = "$urlRest/"
            val expectedBody = practitionerList
            val expectedResponseJSON = ""
            val aidboxClient = createClient(expectedBody, expectedResponseJSON, urlPublish, responseStatus = expectedResponseStatus)
            runBlocking {
                aidboxClient.batchUpsert(practitionerList)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 5xx exception`() {
        val expectedResponseStatus = HttpStatusCode.ServiceUnavailable
        val exception = assertThrows(ServerResponseException::class.java) {
            val urlPublish = "$urlRest/"
            val expectedBody = practitionerList
            val expectedResponseJSON = ""
            val aidboxClient = createClient(expectedBody, expectedResponseJSON, urlPublish, responseStatus = expectedResponseStatus)
            runBlocking {
                aidboxClient.batchUpsert(practitionerList)
            }
        }
        assertEquals(expectedResponseStatus, exception.response.status)
    }

    private fun createClient(
        expectedBody: String,
        responseContent: String,
        expectedUrl: String,
        baseUrl: String = urlRest,
        expectedAuthHeader: String = "Bearer $authString",
        responseStatus: HttpStatusCode = HttpStatusCode.OK
    ): AidboxClient {
        val mockEngine = MockEngine { request ->
            assertEquals(expectedUrl, request.url.toString())
            assert(expectedUrl.startsWith(baseUrl, ignoreCase = true))
            assertEquals(expectedBody, String(request.body.toByteArray()))
            assertEquals(expectedAuthHeader, request.headers["Authorization"])
            respond(
                content = responseContent,
                status = responseStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
        }

        return AidboxClient(httpClient, baseUrl, authString)
    }
}
