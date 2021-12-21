package com.projectronin.interop.aidbox.utils

import com.projectronin.interop.aidbox.client.model.GraphQLError
import com.projectronin.interop.aidbox.client.model.GraphQLResponse
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.RedirectResponseException
import io.ktor.client.features.ServerResponseException
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
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.MovedPermanently
        coEvery { httpResponse.receive<String>() } returns "Moved to http://localhost:9000"

        val exception = RedirectResponseException(httpResponse, "Error")

        val response = runBlocking {
            respondToException<String>(exception)
        }

        val expectedError =
            GraphQLError("Error communicating with Aidbox. Received status code 301 Moved Permanently with message \"Moved to http://localhost:9000\"")
        val expectedResponse = GraphQLResponse<String>(errors = listOf(expectedError))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `respondToException handles ClientRequestException`() {
        val httpResponse = mockk<HttpResponse>()
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.Unauthorized
        coEvery { httpResponse.receive<String>() } returns "Unauthorized"

        val exception = ClientRequestException(httpResponse, "Error")

        val response = runBlocking {
            respondToException<String>(exception)
        }

        val expectedError =
            GraphQLError("Error communicating with Aidbox. Received status code 401 Unauthorized with message \"Unauthorized\"")
        val expectedResponse = GraphQLResponse<String>(errors = listOf(expectedError))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `respondToException handles ServerResponseException`() {
        val httpResponse = mockk<HttpResponse>()
        every { httpResponse.call.request.url } returns Url("http://localhost")
        every { httpResponse.status } returns HttpStatusCode.InternalServerError
        coEvery { httpResponse.receive<String>() } returns "Server Error"

        val exception = ServerResponseException(httpResponse, "Error")

        val response = runBlocking {
            respondToException<String>(exception)
        }

        val expectedError =
            GraphQLError("Error communicating with Aidbox. Received status code 500 Internal Server Error with message \"Server Error\"")
        val expectedResponse = GraphQLResponse<String>(errors = listOf(expectedError))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `respondToException handles unsupported exception`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                respondToException<String>(IllegalStateException())
            }
        }
    }
}
