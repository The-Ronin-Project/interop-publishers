package com.projectronin.interop.aidbox.client.model

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.aidbox.model.TestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraphQLResponseTest {
    private val objectMapper = jsonMapper {
        addModule(kotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Test
    fun `check getters`() {
        val error = GraphQLError("Message")
        val response = GraphQLResponse("Data", listOf(error), mapOf("key" to "value"))
        assertEquals("Data", response.data)
        assertEquals(listOf(error), response.errors)
        assertEquals(mapOf("key" to "value"), response.extensions)
    }

    @Test
    fun `can deserialize simple data`() {
        val json = """
            |{
            |   "data": "value"
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<String>>(json)

        val expectedResponse = GraphQLResponse(data = "value")
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `can deserialize complex data`() {
        val json = """
            |{
            |   "data": {
            |       "id": "UUID-1",
            |       "name": "Value 1"
            |   }
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<TestData>>(json)

        val expectedData = TestData("UUID-1", "Value 1")
        val expectedResponse = GraphQLResponse(data = expectedData)
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `can deserialize list of data`() {
        val json = """
            |{
            |   "data": [
            |       {
            |           "id": "UUID-1",
            |           "name": "Value 1"
            |       },
            |       {
            |           "id": "UUID-2",
            |           "name": "Value 2"
            |       }
            |   ]
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<List<TestData>>>(json)

        val expectedData1 = TestData("UUID-1", "Value 1")
        val expectedData2 = TestData("UUID-2", "Value 2")
        val expectedResponse = GraphQLResponse(data = listOf(expectedData1, expectedData2))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `can deserialize single error`() {
        val json = """
            |{
            |   "errors": [
            |       {
            |           "message": "An error occurred"
            |       }
            |   ]
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<Any>>(json)

        val expectedError = GraphQLError("An error occurred")
        val expectedResponse = GraphQLResponse<Any>(errors = listOf(expectedError))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `can deserialize multiple errors`() {
        val json = """
            |{
            |   "errors": [
            |       {
            |           "message": "An error occurred"
            |       },
            |       {
            |           "message": "Another error occurred"
            |       }
            |   ]
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<Any>>(json)

        val expectedError1 = GraphQLError("An error occurred")
        val expectedError2 = GraphQLError("Another error occurred")
        val expectedResponse = GraphQLResponse<Any>(errors = listOf(expectedError1, expectedError2))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `can deserialize single extension`() {
        val json = """
            |{
            |   "extensions": {
            |       "name": "MY_EXTENSION"
            |   }
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<Any>>(json)

        val expectedResponse = GraphQLResponse<Any>(extensions = mapOf("name" to "MY_EXTENSION"))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `can deserialize multiple extensions`() {
        val json = """
            |{
            |   "extensions": {
            |       "name": "MY_EXTENSION",
            |       "extension2": "Another extension"
            |   }
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<Any>>(json)

        val expectedResponse =
            GraphQLResponse<Any>(extensions = mapOf("name" to "MY_EXTENSION", "extension2" to "Another extension"))
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `can deserialize full object`() {
        val json = """
            |{
            |   "data": {
            |       "id": "UUID-1",
            |       "name": "Value 1"
            |   },
            |   "errors": [
            |       {
            |           "message": "An error occurred"
            |       }
            |   ],
            |   "extensions": {
            |       "name": "MY_EXTENSION"
            |   }
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<TestData>>(json)

        val expectedData = TestData("UUID-1", "Value 1")
        val expectedError = GraphQLError("An error occurred")
        val expectedResponse = GraphQLResponse(
            data = expectedData,
            errors = listOf(expectedError),
            extensions = mapOf("name" to "MY_EXTENSION")
        )
        assertEquals(expectedResponse, response)
    }

    @Test
    fun `ignores unknown fields`() {
        val json = """
            |{
            |   "data": {
            |       "id": "UUID-1",
            |       "name": "Value 1"
            |   },
            |   "moreData": "Hello"
            |}""".trimMargin()
        val response = objectMapper.readValue<GraphQLResponse<TestData>>(json)

        val expectedData = TestData("UUID-1", "Value 1")
        val expectedResponse = GraphQLResponse(data = expectedData)
        assertEquals(expectedResponse, response)
    }
}
