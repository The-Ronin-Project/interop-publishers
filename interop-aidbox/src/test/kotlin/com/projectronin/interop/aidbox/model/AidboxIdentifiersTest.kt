package com.projectronin.interop.aidbox.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AidboxIdentifiersTest {

    @Test
    fun `can deserialize`() {
        val json = """
            {
              "id" : "udp",
              "identifier" : [ {
                "system" : "yes",
                "value" : "sure"
              } ]
            }
        """.trimIndent()
        val deserialized = JacksonManager.objectMapper.readValue<AidboxIdentifiers>(json)
        assertEquals("udp", deserialized.udpId)
        assertEquals("yes", deserialized.identifiers.first().system?.value)
        assertEquals("sure", deserialized.identifiers.first().value?.value)
    }

    @Test
    fun `can serialize`() {
        val identifier = AidboxIdentifiers(
            udpId = "udp",
            identifiers = listOf(
                Identifier(value = "sure".asFHIR(), system = Uri(value = "yes"))
            )
        )
        val actualJson = JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(identifier)
        val expectedJson = """
            {
              "id" : "udp",
              "identifier" : [ {
                "system" : "yes",
                "value" : "sure"
              } ]
            }
        """.trimIndent()
        assertEquals(expectedJson, actualJson)
    }
}
