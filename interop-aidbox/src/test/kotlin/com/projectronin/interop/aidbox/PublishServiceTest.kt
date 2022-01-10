package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PublishServiceTest {
    private val practitionerList = this::class.java.getResource("/json/AidboxPractitionerList.json")!!.readText()
    private lateinit var aidboxClient: AidboxClient
    private lateinit var publishService: PublishService

    @BeforeEach
    fun initTest() {
        aidboxClient = mockk<AidboxClient>()
        publishService = PublishService(aidboxClient)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 200 true`() {
        val expectedSuccess = true
        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.status } returns HttpStatusCode.OK
        coEvery { aidboxClient.batchUpsert(practitionerList) } returns httpResponse
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, response 1xx (or 2xx) false`() {
        val expectedSuccess = false
        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.status } returns HttpStatusCode.Continue
        coEvery { aidboxClient.batchUpsert(practitionerList) } returns httpResponse
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish array of 3 Practitioner resources to aidbox, exception 5xx (or 3xx or 4xx) false`() {
        val expectedSuccess = false
        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.status } returns HttpStatusCode.ServiceUnavailable
        coEvery { aidboxClient.batchUpsert(practitionerList) } returns httpResponse
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(practitionerList)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }
}
