package com.projectronin.interop.datalake

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.datalake.azure.client.AzureClient
import com.projectronin.interop.fhir.r4.resource.Resource
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Service allowing access to push data updates to  the datalake
 */
@Service
class PublishService(private val azureClient: AzureClient) {
    private val logger = KotlinLogging.logger { }

    /**
     * Publishes serialized FHIR R4 resources to the Azure datalake.
     *
     * Gets the resourceType and resourceId from the FHIR R4 resource content.
     * A resourceType is a name defined by the FHIR spec. Examples: Patient, Practitioner, Condition, Observation, etc.
     * A resourceId could be any valid FHIR id.
     *
     * Each resource in [resources] is published to a distinct file in the datalake container.
     * The file path supports Data Platform needs for code optimization and bronze directory layout:
     * "/fhir-r4/tenant_id={tenantId}/date={today's date}/resource_type={resourceType}/{resourceId}.json"
     *
     * @param tenantId The tenant mnemonic for the specific resource
     * @param resources List of FHIR resources to publish. May be a mixed List with different resourceTypes,
     *                  but expects all of them to have a defined ID
     * @return true for success; success may include no data to publish
     */
    fun publishFHIRR4(tenantId: String, resources: List<Resource<*>>): Boolean {
        val root = "/fhir-r4"
        logger.info { "Publishing Ronin clinical data to datalake at $root" }
        if (resources.isEmpty()) {
            logger.debug { "Publishing nothing to datalake because the supplied data is empty" }
            return true
        }
        val dateOfExport = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        resources.forEach {
            val resourceType = it.resourceType
            val resourceId = it.id?.value
                ?: throw IllegalStateException(
                    "Attempted to publish a ${it.resourceType} resource without a FHIR ID for tenant $tenantId"
                )
            val filePathString = "$root/date=$dateOfExport/tenant_id=$tenantId/resource_type=$resourceType/$resourceId.json"
            logger.debug { "Publishing Ronin clinical data to $filePathString" }
            val serialized = JacksonManager.objectMapper.writeValueAsString(it)
            azureClient.upload(filePathString, serialized)
        }
        return true
    }

    /**
     * Publishes serialized JSON data to the Azure datalake.
     * The [data] is expected to be the response payload from an API call whose response content format is JSON.
     *
     * For JSON response data from FHIR APIs, use publishFHIRR4().
     * For JSON response data from all other APIs, use publishAPIJSON().
     *
     * publishAPIJSON() publishes the JSON data to a file in the datalake container.
     * The file path supports Data Platform needs for code optimization and bronze directory layout:
     * "/api-json/schema={schema}/tenant_id={tenantId}/date={today’s date}/{millisecond timestamp}.json"
     *
     * The data schema is defined by the API, so this method requires input of the [method] and [url]
     * to uniquely identify the API call. When writing the data, publishAPIJSON() joins these input values
     * with a hyphen to generate a label for the schema segment of the file path.
     *
     * The file name in the path replaces punctuation in the millisecond timestamp with hyphens.
     *
     * @param tenantId The tenant mnemonic
     * @param data Serialized JSON response data from an API call
     * @param method Method for the API call, example: GET, POST
     * @param url URL for the API call
     * @return true for success; success may include no data to publish
     */
    fun publishAPIJSON(tenantId: String, data: String, method: String, url: String): Boolean {
        val root = "/api-json"
        logger.info { "Publishing Ronin clinical data to datalake at $root" }
        if (method.isEmpty() || url.isEmpty()) {
            throw IllegalStateException(
                "Attempted to publish JSON data from an API response without identifying the API request for tenant $tenantId"
            )
        }
        if (data.isEmpty()) {
            logger.debug { "Publishing nothing to datalake because the supplied data is empty" }
            return true
        }
        val pathCleanup: Regex = "[^A-Za-z0-9_-]+".toRegex()
        // Note: schema contributes to a file path limit of 1024 in the bronze directory. No length issues are expected.
        val schema = "$method-$url"
            .replace(pathCleanup, "")
        val dateOfExport = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timeOfExport = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
            .replace(pathCleanup, "-")
        val filePathString = "$root/schema=$schema/date=$dateOfExport/tenant_id=$tenantId/$timeOfExport.json"
        logger.debug { "Publishing Ronin clinical data to $filePathString" }
        azureClient.upload(filePathString, data)
        return true
    }
}
