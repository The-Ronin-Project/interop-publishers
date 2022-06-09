package com.projectronin.interop.aidbox.utils

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AidboxUtilsTest {
    @BeforeEach
    fun setup() {
        mockkStatic("io.ktor.client.statement.HttpResponseKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("io.ktor.client.statement.HttpResponseKt")
    }

    @Test
    fun `respondToException handles RedirectResponseException`() {
        val httpResponse = mockk<HttpResponse>()
        every { httpResponse.call.request.method.value } returns "Method"
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.MovedPermanently
        coEvery { httpResponse.bodyAsText() } returns "Moved to http://localhost:9000"

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
        coEvery { httpResponse.bodyAsText() } returns "Unauthorized"

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
        coEvery { httpResponse.bodyAsText() } returns "Server Error"

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
