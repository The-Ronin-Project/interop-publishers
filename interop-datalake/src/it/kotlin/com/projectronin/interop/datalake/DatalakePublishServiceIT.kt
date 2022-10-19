package com.projectronin.interop.datalake

import com.google.common.base.Optional
import com.oracle.bmc.Region
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.datalake.oci.auth.OCIConfiguration
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import io.mockk.every
import io.mockk.mockk
import org.bouncycastle.util.encoders.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Testcontainers
class DatalakePublishServiceIT {
    companion object {
        @Container
        private val mockserver =
            GenericContainer("docker-proxy.devops.projectronin.io/mockserver/mockserver").withExposedPorts(1080)
                .waitingFor(Wait.forLogMessage(".*started on port: 1080.*", 1))

        fun getPort() = mockserver.getMappedPort(1080)

        fun getMockserverUrl() = "http://localhost:${getPort()}"
    }

    private val client = MockServerClient("localhost", getPort())

    private val namespace = "dataplatform"
    private val bucketName = "interops-data"
    private val ociTenantId = "ociTenant"
    private val ociUserId = "ociUser"
    private val fingerPrint = "fingerPrint"
    private val privateKey =
        java.util.Base64.getEncoder().encodeToString(
            this::class.java.getResource("/ExamplePEMPrivateKey.txt")!!.readText().replace("\\r", "")
                .replace("\\n", "")
                .toByteArray()
        )

    // We have to mock the region to force this to our localhost.
    private val region = mockk<Region> {
        every { getEndpoint(ObjectStorageClient.SERVICE) } returns Optional.of(getMockserverUrl())
    }

    private val credentials = OCIConfiguration(ociTenantId, ociUserId, fingerPrint, privateKey, namespace, bucketName, region)
    private val ociClient = OCIClient(credentials)
    private val publishService = DatalakePublishService(ociClient)

    @BeforeEach
    fun setup() {
        client.reset()
    }

    @Test
    fun `can publish FHIR`() {
        val id = Id(UUID.randomUUID().toString())
        val patient = Patient(
            id = id
        )
        val tenant = "ronin"

        val objectName = getR4Name(tenant, "Patient", id.value)
        setPutExpectation(objectName)

        publishService.publishFHIRR4(tenant, listOf(patient))

        val request = getLastPut(objectName)
        request!!
        assertEquals(objectMapper.writeValueAsString(patient), decode(request.bodyAsString))
    }

    private fun getR4Name(tenantId: String, resourceType: String, resourceId: String): String {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return URLEncoder.encode(
            "fhir-r4/date=$date/tenant_id=$tenantId/resource_type=$resourceType/$resourceId.json",
            "UTF-8"
        )
    }

    private fun setPutExpectation(objectName: String) {
        client.`when`(
            request()
                .withMethod("PUT")
                .withPath(getObjectPath(objectName))
        ).respond(
            HttpResponse.response().withStatusCode(200)
        )
    }

    private fun getLastPut(objectName: String): HttpRequest? =
        client.retrieveRecordedRequests(
            request().withPath(getObjectPath(objectName)).withMethod("PUT")
        ).firstOrNull()

    private fun getObjectPath(objectName: String) = "/n/$namespace/b/$bucketName/o/$objectName"

    private fun decode(data: String) = String(Base64.decode(data))
}
