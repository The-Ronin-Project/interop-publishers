package com.projectronin.interop.aidbox

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.projectronin.interop.aidbox.spring.AidboxSpringConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.resource.Bundle
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [AidboxSpringConfig::class])
@AidboxData("/aidbox/patient/Patients.yaml")
class AidboxClientIntegrationTest : BaseAidboxTest() {
    companion object {
        // allows us to dynamically change the aidbox port to the testcontainer instance
        @JvmStatic
        @DynamicPropertySource
        fun aidboxUrlProperties(registry: DynamicPropertyRegistry) {
            registry.add("aidbox.url") { aidbox.baseUrl() }
            registry.add("aidbox.client.id") { aidbox.aidboxClientId }
            registry.add("aidbox.client.secret") { aidbox.aidboxClientSecret }
        }
    }

    @Test
    fun `search works`() {
        val response = search("Patient", "mdaoc", "my-own-system|abcdef")
        val entries = response!!.entry
        assertEquals(1, entries.size)
        assertEquals("mdaoc-12345678902", entries.first().resource?.id?.value)
    }

    private fun search(resourceType: String, tenant: String, identifierToken: String): Bundle? {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val tenantIdentifier = "${CodeSystem.RONIN_TENANT.uri.value}|$tenant".encodeURLPathPart()
            val identifierTokenEncode = identifierToken.encodeURLPathPart()
            val url = "$aidboxUrl/fhir/$resourceType?identifier=$tenantIdentifier&identifier=$identifierTokenEncode"
            val response = try {
                aidbox.ktorClient.get(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
                e.response
            }

            if (response.status.isSuccess()) {
                val objectNode = response.body<ObjectNode>()
                JacksonManager.objectMapper.convertValue<Bundle>(objectNode)
            } else if (response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw IllegalStateException("Error while purging test data: ${response.bodyAsText()}")
            }
        }
    }
}
