package com.projectronin.interop.aidbox.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.projectronin.interop.aidbox.client.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.TestData
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AidboxClientTest {
    private val testUrl = "http://localhost/test/graphql"
    private val authString = "Auth-String"

    @Test
    fun `request with no parameters`() {
        val query = """
            |query Test {
            |   test {
            |       id,
            |       name
            |   }
            |}
        """.trimMargin()
        val expectedBody =
            """{"query":"query Test {\n   test {\n       id,\n       name\n   }\n}","operationName":null,"variables":{}}"""
        val expectedResponseJSON = """
            |{
            |   "data": {
            |       "test": [
            |           {
            |               "id": "id1",
            |               "name": "name 1"
            |           },
            |           {
            |               "id": "id2",
            |               "name": "name 2"
            |           }
            |       ]
            |   }
            |}
            """.trimMargin()

        val aidboxClient = createClient(expectedBody, expectedResponseJSON)

        val response = runBlocking {
            val httpResponse = aidboxClient.query(query, authString)
            httpResponse.receive<GraphQLResponse<TestDataContainer>>()
        }

        val expectedData1 = TestData("id1", "name 1")
        val expectedData2 = TestData("id2", "name 2")
        val expectedResponse: GraphQLResponse<TestDataContainer> =
            GraphQLResponse(data = TestDataContainer(listOf(expectedData1, expectedData2)))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `request with parameterized query and single variable`() {
        val query = """
            |query Test(${"$"}name: String!) {
            |   test(name: ${"$"}name) {
            |       id,
            |       name
            |   }
            |}
        """.trimMargin()
        val expectedBody =
            """{"query":"query Test(${"$"}name: String!) {\n   test(name: ${"$"}name) {\n       id,\n       name\n   }\n}","operationName":null,"variables":{"name":"name 1"}}"""
        val expectedResponseJSON = """
            |{
            |   "data": {
            |       "test": [
            |           {
            |               "id": "id1",
            |               "name": "name 1"
            |           }
            |       ]
            |   }
            |}
            """.trimMargin()

        val aidboxClient = createClient(expectedBody, expectedResponseJSON)

        val response = runBlocking {
            val httpResponse = aidboxClient.query(query, authString, parameters = mapOf("name" to "name 1"))
            httpResponse.receive<GraphQLResponse<TestDataContainer>>()
        }

        val expectedData1 = TestData("id1", "name 1")
        val expectedResponse =
            GraphQLResponse<TestDataContainer>(data = TestDataContainer(listOf(expectedData1)))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `request with parameterized query and multiple variables`() {
        val query = """
            |query Test(${"$"}name: String!, ${"$"}id: String!) {
            |   test(name: ${"$"}name, id: ${"$"}id) {
            |       id,
            |       name
            |   }
            |}
        """.trimMargin()
        val expectedBody =
            """{"query":"query Test(${"$"}name: String!, ${"$"}id: String!) {\n   test(name: ${"$"}name, id: ${"$"}id) {\n       id,\n       name\n   }\n}","operationName":null,"variables":{"id":"id1","name":"name 1"}}"""
        val expectedResponseJSON = """
            |{
            |   "data": {
            |       "test": [
            |           {
            |               "id": "id1",
            |               "name": "name 1"
            |           }
            |       ]
            |   }
            |}
            """.trimMargin()

        val aidboxClient = createClient(expectedBody, expectedResponseJSON)

        val response = runBlocking {
            val httpResponse = aidboxClient.query(
                query,
                authString,
                parameters = mapOf("name" to "name 1", "id" to "id1")
            )
            httpResponse.receive<GraphQLResponse<TestDataContainer>>()
        }

        val expectedData1 = TestData("id1", "name 1")
        val expectedResponse =
            GraphQLResponse<TestDataContainer>(data = TestDataContainer(listOf(expectedData1)))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `request returns non-200`() {
        val exception = assertThrows(ClientRequestException::class.java) {
            val query = """
            |query Test {
            |   test {
            |       id,
            |       name
            |   }
            |}
        """.trimMargin()
            val expectedBody =
                """{"query":"query Test {\n   test {\n       id,\n       name\n   }\n}","operationName":null,"variables":{}}"""

            val aidboxClient = createClient(expectedBody, "", status = HttpStatusCode.Unauthorized)

            runBlocking {
                aidboxClient.query(query, authString)
            }
        }

        assertEquals(HttpStatusCode.Unauthorized, exception.response.status)
    }

    private fun createClient(
        expectedBody: String,
        response: String,
        url: String = testUrl,
        expectedAuthHeader: String = "Bearer $authString",
        status: HttpStatusCode = HttpStatusCode.OK
    ): AidboxClient {
        val mockEngine = MockEngine { request ->
            assertEquals(url, request.url.toString())
            assertEquals(expectedBody, String(request.body.toByteArray()))
            assertEquals(expectedAuthHeader, request.headers["Authorization"])

            respond(
                content = response,
                status = status,
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
        return AidboxClient(httpClient, url)
    }
}

data class TestDataContainer(val test: List<TestData>)
