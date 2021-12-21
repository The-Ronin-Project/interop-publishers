package com.projectronin.interop.aidbox.client.model

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraphQLErrorTest {
    private val objectMapper = jsonMapper {
        addModule(kotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Test
    fun `check getters`() {
        val location = GraphQLErrorLocation(5, 7)
        val error = GraphQLError("Message", listOf(location), listOf("1", 2), mapOf("key" to "value"))
        assertEquals("Message", error.message)
        assertEquals(listOf(location), error.locations)
        assertEquals(listOf("1", 2), error.path)
        assertEquals(mapOf("key" to "value"), error.extensions)
    }

    @Test
    fun `can deserialize with just message`() {
        val json = """
            |{
            |   "message": "An error occurred"
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedError = GraphQLError("An error occurred")
        assertEquals(expectedError, error)
    }

    @Test
    fun `can deserialize single location`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "locations": [
            |       {
            |           "line": 5,
            |           "column": 7
            |       }
            |   ]
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedLocation = GraphQLErrorLocation(5, 7)
        val expectedError = GraphQLError("An error occurred", locations = listOf(expectedLocation))
        assertEquals(expectedError, error)
    }

    @Test
    fun `can deserialize multiple locations`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "locations": [
            |       {
            |           "line": 5,
            |           "column": 7
            |       },
            |       {
            |           "line": 10,
            |           "column": 25
            |       }
            |   ]
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedLocation1 = GraphQLErrorLocation(5, 7)
        val expectedLocation2 = GraphQLErrorLocation(10, 25)
        val expectedError = GraphQLError("An error occurred", locations = listOf(expectedLocation1, expectedLocation2))
        assertEquals(expectedError, error)
    }

    @Test
    fun `can deserialize single path`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "path": ["test"]
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedError = GraphQLError("An error occurred", path = listOf("test"))
        assertEquals(expectedError, error)
    }

    @Test
    fun `can deserialize multiple paths`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "path": ["test", "name", 1, "first"]
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedError = GraphQLError("An error occurred", path = listOf("test", "name", 1, "first"))
        assertEquals(expectedError, error)
    }

    @Test
    fun `can deserialize single extension`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "extensions": {
            |       "code": "CAN_NOT_FETCH_BY_ID"
            |   }
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedError = GraphQLError("An error occurred", extensions = mapOf("code" to "CAN_NOT_FETCH_BY_ID"))
        assertEquals(expectedError, error)
    }

    @Test
    fun `can deserialize multiple extensions`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "extensions": {
            |       "code": "CAN_NOT_FETCH_BY_ID",
            |       "timestamp": "Fri Feb 9 14:33:09 UTC 2018"
            |   }
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedError = GraphQLError(
            "An error occurred",
            extensions = mapOf("code" to "CAN_NOT_FETCH_BY_ID", "timestamp" to "Fri Feb 9 14:33:09 UTC 2018")
        )
        assertEquals(expectedError, error)
    }

    @Test
    fun `can deserialize full object`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "locations": [
            |       {
            |           "line": 5,
            |           "column": 7
            |       }
            |   ],
            |   "path": ["test", "name", 1, "first"],
            |   "extensions": {
            |       "code": "CAN_NOT_FETCH_BY_ID"
            |   }
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedLocation = GraphQLErrorLocation(5, 7)
        val expectedError = GraphQLError(
            "An error occurred",
            locations = listOf(expectedLocation),
            path = listOf("test", "name", 1, "first"),
            extensions = mapOf("code" to "CAN_NOT_FETCH_BY_ID")
        )
        assertEquals(expectedError, error)
    }

    @Test
    fun `ignores unknown fields`() {
        val json = """
            |{
            |   "message": "An error occurred",
            |   "code": "CAN_NOT_FETCH_BY_ID"
            |}""".trimMargin()
        val error = objectMapper.readValue<GraphQLError>(json)

        val expectedError = GraphQLError("An error occurred")
        assertEquals(expectedError, error)
    }
}
