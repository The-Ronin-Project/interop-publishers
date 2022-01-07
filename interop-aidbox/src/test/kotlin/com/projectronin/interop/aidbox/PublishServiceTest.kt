package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PublishServiceTest {
    private val urlRest = "http://localhost/8888"
    private val authString = "Auth-String"
    private val practitionerList = this::class.java.getResource("/json/AidboxPractitionerList.json")!!.readText()

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 200 true`() {
        val expectedSuccess = true
        val publishService = createService(practitionerList, HttpStatusCode.OK)
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 1xx false`() {
        val expectedSuccess = false
        val publishService = createService("", HttpStatusCode.Continue)
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 2xx false`() {
        val expectedSuccess = false
        val publishService = createService("", HttpStatusCode.Accepted)
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 3xx false`() {
        val expectedSuccess = false
        val publishService = createService("", HttpStatusCode.TemporaryRedirect)
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 4xx false`() {
        val expectedSuccess = false
        val publishService = createService("", HttpStatusCode.Unauthorized)
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 5xx false`() {
        val expectedSuccess = false
        val publishService = createService("", HttpStatusCode.ServiceUnavailable)
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    private fun createService(
        body: String,
        responseStatus: HttpStatusCode = HttpStatusCode.OK
    ): PublishService {
        val mockEngine = MockEngine { _ ->
            respond(
                content = body,
                status = responseStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
        }
        val aidboxClient = AidboxClient(httpClient, urlRest, authString)

        return PublishService(aidboxClient)
    }
}
