package com.projectronin.interop.aidbox.utils

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AidboxUtilsTest {
    @Test
    fun `respondToException handles RedirectResponseException`() {
        val httpResponse = mockk<HttpResponse>()
        every { httpResponse.call.request.method.value } returns "Method"
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.MovedPermanently
        coEvery { httpResponse.body<String>() } returns "Moved to http://localhost:9000"

        val exception = RedirectResponseException(httpResponse, "Error")
        val response = runBlocking {
            respondToException<String>(exception)
        }

        assertEquals(httpResponse, response)
    }

    @Test
    fun `respondToException handles ClientRequestException`() {
        val httpResponse = mockk<HttpResponse>()
        every { httpResponse.call.request.method.value } returns "Method"
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.Unauthorized
        coEvery { httpResponse.body<String>() } returns "Unauthorized"

        val exception = ClientRequestException(httpResponse, "Error")
        val response = runBlocking {
            respondToException<String>(exception)
        }

        assertEquals(httpResponse, response)
    }

    @Test
    fun `respondToException handles ServerResponseException`() {
        val httpResponse = mockk<HttpResponse>()
        every { httpResponse.call.request.method.value } returns "Method"
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.InternalServerError
        coEvery { httpResponse.body<String>() } returns "Server Error"

        val exception = ServerResponseException(httpResponse, "Error")
        val response = runBlocking {
            respondToException<String>(exception)
        }

        assertEquals(httpResponse, response)
    }

    @Test
    fun `respondToException handles unsupported exception`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                respondToException<String>(IllegalStateException())
            }
        }
    }

    @Test
    fun `respondToGraphQLException handles unsupported exception`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                respondToGraphQLException<String>(IllegalStateException())
            }
        }
    }
}
