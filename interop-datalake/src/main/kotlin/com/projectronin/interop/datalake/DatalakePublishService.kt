package com.projectronin.interop.datalake

import com.projectronin.interop.common.hl7.EventType
import com.projectronin.interop.common.hl7.MessageType
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.datalake.hl7.getMSH9
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.resource.Resource
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Service allowing access to push data updates to the datalake
 */
@Service
class DatalakePublishService(private val ociClient: OCIClient) {
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
     * @throws IllegalStateException if any of the resources lacked FHIR id values so were not published.
     */
    fun publishFHIRR4(tenantId: String, resources: List<Resource<*>>) {
        val root = "/fhir-r4"
        logger.info { "Publishing Ronin clinical data to datalake at $root" }
        if (resources.isEmpty()) {
            logger.debug { "Publishing nothing to datalake because the supplied data is empty" }
            return
        }
        val dateOfExport = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val resourcesToWrite = resources.filter { it.id?.value?.isNotEmpty() ?: false }

        resourcesToWrite.forEach {
            val resourceType = it.resourceType
            val resourceId = it.id?.value
            val filePathString =
                "$root/date=$dateOfExport/tenant_id=$tenantId/resource_type=$resourceType/$resourceId.json"
            logger.debug { "Publishing Ronin clinical data to $filePathString" }
            val serialized = JacksonManager.objectMapper.writeValueAsString(it)
            ociClient.upload(filePathString, serialized)
        }

        if (resourcesToWrite.size < resources.size) {
            throw IllegalStateException(
                "Did not publish all FHIR resources to datalake for tenant $tenantId: Some resources lacked FHIR IDs. Errors were logged."
            )
        }
    }

    /**
     * Publishes serialized JSON data to the OCI datalake.
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
     * @throws IllegalStateException if the method or url is empty so the API request could not be correctly identified.
     */
    fun publishAPIJSON(tenantId: String, data: String, method: String, url: String) {
        val root = "/api-json"
        logger.info { "Publishing Ronin clinical data to datalake at $root" }
        if (method.isEmpty() || url.isEmpty()) {
            throw IllegalStateException(
                "Attempted to publish JSON data from an API response without identifying the API request for tenant $tenantId"
            )
        }
        if (data.isEmpty()) {
            logger.debug { "Publishing nothing to datalake because the supplied data is empty" }
            return
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
        ociClient.upload(filePathString, data)
    }

    /**
     * Publishes HL7v2 data to the OCI datalake.
     *
     * publishHL7v2() publishes the HL7v2 data to a file in the datalake container.
     * The file path supports Data Platform needs for code optimization and bronze directory layout:
     * "/hl7v2/tenant_id={tenantId}/date={today's date}/message_type={messageType}/message_event={messageEvent}/{millisecond timestamp}.hl7"
     *
     * Gets messageType and messageEvent from the HL7v2 message MSH segment.
     * The file name in the path replaces punctuation in the millisecond timestamp with hyphens.
     *
     * @param tenantId The tenant mnemonic
     * @param messages List of HL7v2 messages to publish. May be a mix of different message types and message events.
     * @throws IllegalStateException if any of the HL7v2 messages had an invalid structure so could not be published.
     */
    fun publishHL7v2(tenantId: String, messages: List<String>) {
        val root = "/hl7v2"
        logger.info { "Publishing Ronin clinical data to datalake at $root" }
        if (messages.isEmpty()) {
            logger.debug { "Publishing nothing to datalake because the supplied data is empty" }
            return
        }
        val pathCleanup: Regex = "[^A-Za-z0-9_-]+".toRegex()
        val dateOfExport = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timeOfExport = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
            .replace(pathCleanup, "-")

        val messagesToWrite = messages.filter { message -> message.isNotEmpty() }
        var messageCount = messagesToWrite.size

        messagesToWrite.forEachIndexed { index, message ->
            val forEachTimeOfExport = "$timeOfExport-$index"
            val messageStructure = getMSH9(tenantId, message)
            if (messageStructure.count() < 2) {
                logger.error {
                    "Did not publish HL7v2 message to datalake for tenant $tenantId: the message has invalid structure at index $forEachTimeOfExport"
                }
                messageCount--
                return@forEachIndexed
            }
            val messageType = messageStructure[0]
            val messageEvent = "$messageType${messageStructure[1]}"
            val filePathString =
                "$root/date=$dateOfExport/tenant_id=$tenantId/message_type=$messageType/message_event=$messageEvent/$forEachTimeOfExport.json"
            if ((MessageType.values().find { it.toString() == messageType } == null) ||
                (EventType.values().find { it.toString() == messageEvent } == null)
            ) {
                logger.error {
                    "Did not publish HL7v2 message to datalake at $filePathString for tenant $tenantId: $messageEvent messages are not supported at index $forEachTimeOfExport"
                }
                messageCount--
                return@forEachIndexed
            }
            logger.debug { "Publishing Ronin clinical data to $filePathString" }
            ociClient.upload(filePathString, message)
        }
        if (messageCount < messagesToWrite.size) {
            throw IllegalStateException(
                "Did not publish all HL7v2 messages to datalake for tenant $tenantId: Problems with some message structures. Errors were logged."
            )
        }
    }
}