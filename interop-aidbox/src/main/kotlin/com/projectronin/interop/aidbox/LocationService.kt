package com.projectronin.interop.aidbox

import com.fasterxml.jackson.annotation.JsonProperty
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.AIDBOX_LOCATION_FHIR_IDS_QUERY
import com.projectronin.interop.aidbox.utils.respondToGraphQLException
import com.projectronin.interop.common.exceptions.LogMarkingException
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import datadog.trace.api.Trace
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class LocationService(
    private val aidboxClient: AidboxClient,
    @Value("\${aidbox.batchSize:100}") private val batchSize: Int
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Searches Aidbox for Locations based on their [tenantMnemonic] and a map of keys to [SystemValue]s, each
     * representing a Location identifier.  Returns a map of the given keys, along with the Location's FHIR ID
     * if it was found, otherwise no entry for that key.
     */
    @Trace
    fun <K> getLocationFHIRIds(tenantMnemonic: String, identifiers: Map<K, SystemValue>): Map<K, String> {
        logger.info { "Retrieving Location FHIR IDs from Aidbox" }

        val collectedLocations = identifiers.values.chunked(batchSize).flatMap { batch ->
            queryLocationFHIRIds(tenantMnemonic, batch).data?.locationList ?: emptyList()
        }

        val identifierToFHIRIdMap = collectedLocations.flatMap { location ->
            location.identifiers.map { it to location.id }
        }.toMap()

        return identifiers.mapNotNull {
            val fhirId = identifierToFHIRIdMap[Identifier(system = Uri(it.value.system), value = it.value.value)]

            if (fhirId != null) {
                it.key to fhirId
            } else {
                null
            }
        }.toMap()
    }

    private fun queryLocationFHIRIds(
        tenantMnemonic: String,
        batch: List<SystemValue>
    ): GraphQLResponse<LimitedLocationsFHIR> {
        val query = AIDBOX_LOCATION_FHIR_IDS_QUERY
        val parameters = mapOf(
            "tenant" to SystemValue(
                system = "http://projectronin.com/id/tenantId",
                value = tenantMnemonic
            ).queryString,
            "identifiers" to batch.joinToString(separator = ",") { it.queryString }
        )
        val response: GraphQLResponse<LimitedLocationsFHIR> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.body()
            } catch (e: LogMarkingException) {
                logger.warn(e.logMarker) { "Encountered exception when requesting Location FHIR IDs from Aidbox: ${e.message}" }
                respondToGraphQLException(e)
            }
        }
        return response
    }
}

data class LimitedLocationsFHIR(@JsonProperty("LocationList") val locationList: List<LimitedLocationFHIRIdentifiers>?)
data class LimitedLocationFHIRIdentifiers(
    val id: String,
    @JsonProperty("identifier") val identifiers: List<Identifier>
)
