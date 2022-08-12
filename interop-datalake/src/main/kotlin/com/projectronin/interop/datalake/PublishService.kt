package com.projectronin.interop.datalake

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.datalake.azure.client.AzureClient
import com.projectronin.interop.fhir.FHIRResource
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Service allowing access to push data updates to  the datalake
 */
@Service
class PublishService(private val azureClient: AzureClient) {
    private val logger = KotlinLogging.logger { }

    /**
     * Publishes FHIR resources to the Azure datalake, each resource is published to a file with the container:
     * "/fhir-r4/{resourceType}/date={today's date}/tenant_id={tenantId}/{resourceId}.json"
     *
     * @param tenantId The tenant mnemonic for the specific resource
     * @param resourceCollection List of FHIR resources to publish. May be a mixed List with different resourceTypes,
     * but expects all of them to have a defined ID
     * @return true for success
     */
    fun publish(tenantId: String, resourceCollection: List<FHIRResource>): Boolean {
        logger.info { "Publishing Ronin clinical data to datalake" }
        if (resourceCollection.isEmpty()) {
            logger.debug { "Publishing no data because the collection is empty" }
            return true
        }
        val dateOfExport = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        resourceCollection.forEach {
            val resourceType = it.resourceType.lowercase()
            val resourceId = it.id?.value
                ?: throw IllegalStateException("Attempted to publish a resource of type ${it.resourceType} without a FHIR ID for tenant $tenantId")
            val filePathString = "/fhir-r4/$resourceType/date=$dateOfExport/tenant_id=$tenantId/$resourceId.json"
            logger.debug { "Publishing Ronin clinical data to $filePathString" }
            val serialized = JacksonManager.objectMapper.writeValueAsString(it)
            azureClient.upload(filePathString, serialized)
        }
        return true
    }
}
