package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.spring.AidboxSpringConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.fhir.r4.resource.Bundle
import com.projectronin.interop.fhir.r4.resource.Patient
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    private lateinit var aidboxClient: AidboxClient

    @Test
    fun `search works`() {
        val response = runBlocking {
            aidboxClient.searchForResources("Patient", "mdaoc", "my-own-system|abcdef").body<Bundle?>()
        }
        val entries = response!!.entry
        assertEquals(1, entries.size)
        assertEquals("mdaoc-12345678902", entries.first().resource?.id?.value)
    }

    @Test
    fun `delete works`() {
        val resourceType = "Patient"
        val udpId = "mdaoc-12345678901"

        val initialResource = runBlocking { aidboxClient.getResource(resourceType, udpId).body<Patient?>() }
        assertNotNull(initialResource)

        val deleteResponse = runBlocking { aidboxClient.deleteResource(resourceType, udpId) }
        assertTrue(deleteResponse.status.isSuccess())

        val exception = assertThrows<ClientFailureException> {
            runBlocking {
                aidboxClient.getResource(resourceType, udpId).body<Patient?>()
            }
        }
        assertTrue(exception.message!!.startsWith("Received 410 Gone"))
    }
}
