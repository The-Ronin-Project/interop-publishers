package com.projectronin.interop.aidbox

import com.fasterxml.jackson.annotation.JsonProperty
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.respondToGraphQLException
import com.projectronin.interop.aidbox.utils.validateTenantIdentifier
import com.projectronin.interop.common.exceptions.LogMarkingException
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Practitioner
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PractitionerService(
    private val aidboxClient: AidboxClient,
    @Value("\${aidbox.batchSize:100}") private val batchSize: Int
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Returns a [List] of [Identifier] for a single practitioner from AidBox.
     * @param tenantMnemonic the mnemonic of the tenant represented by this call.
     * @param practitionerFHIRID the FHIR ID of the practitioner (the key value in AidBox)
     */
    fun getPractitionerIdentifiers(tenantMnemonic: String, practitionerFHIRID: String): List<Identifier> {
        logger.info { "Retrieving Practitioner Identifiers from Aidbox using FHIR ID" }
        val query = javaClass.getResource("/graphql/AidboxLimitedPractitionerIDsQuery.graphql")!!.readText()
        val parameters = mapOf(
            "id" to practitionerFHIRID,
            "tenant" to SystemValue(
                system = "http://projectronin.com/id/tenantId",
                value = tenantMnemonic
            ).queryString
        )
        val response: GraphQLResponse<LimitedPractitioner> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.body()
            } catch (e: LogMarkingException) {
                logger.warn(e.logMarker) {
                    "Encountered exception when requesting Practitioner Identifiers from Aidbox using FHIR ID: ${e.message}"
                }
                respondToGraphQLException(e)
            }
        }
        response.errors?.let { return emptyList() }
        logger.info { "Completed retrieving Practitioner Identifiers from Aidbox using FHIR ID" }
        // Practitioner list will have at most 1 practitioner due to direct id reference.
        return response.data?.practitionerList?.firstOrNull()?.identifier ?: listOf()
    }

    /**
     * Returns a specific [Identifier] for a single practitioner from AidBox.
     * @param tenantMnemonic the mnemonic of the tenant represented by this call.
     * @param practitionerFHIRID the FHIR ID of the practitioner (the key value in AidBox)
     * @param idType is a [CodeableConcept] representing the Identifer to retrieve.
     *  (see CodeableConcepts in package com.projectronin.interop.fhir.r4)
     */
    fun getSpecificPractitionerIdentifier(
        tenantMnemonic: String,
        practitionerFHIRID: String,
        idType: CodeableConcept
    ): Identifier? {
        return getPractitionerIdentifiers(tenantMnemonic, practitionerFHIRID).find { it.type == idType }
    }

    /**
     * Returns a [Map] of FHIR ID to [Identifier] for a [List] of practitioner FHIR ids from AidBox.
     * @param tenantMnemonic the mnemonic of the tenant represented by this call.
     * @param practitionerFHIRIDs a list of FHIR IDs to lookup
     * @return a [Map] where the FHIR ID is the key, and a [List] of [Identifier] is the value.
     */
    fun getPractitionersIdentifiers(
        tenantMnemonic: String,
        practitionerFHIRIDs: List<String>
    ): Map<String, List<Identifier>> {
        val idMap = mutableMapOf<String, List<Identifier>>()
        practitionerFHIRIDs.forEach {
            idMap[it] = getPractitionerIdentifiers(tenantMnemonic, it)
        }
        return idMap
    }

    /**
     * Searches Aidbox for Practitioners based on their [tenantMnemonic] and a map of keys to [SystemValue]s, each
     * representing a Practitioner identifier.  Returns a map of the given keys, along with the practitioner's FHIR ID
     * if it was found, otherwise no entry for that key.
     */
    fun <K> getPractitionerFHIRIds(tenantMnemonic: String, identifiers: Map<K, SystemValue>): Map<K, String> {
        logger.info { "Retrieving Practitioner FHIR IDs from Aidbox" }

        val collectedPractitioners = identifiers.values.chunked(batchSize).flatMap { batch ->
            queryPractitionerFHIRIds(tenantMnemonic, batch).data?.practitionerList ?: emptyList()
        }

        val identifierToFhirIdMap = collectedPractitioners.flatMap {
            it.identifiers.map { identifier -> identifier to it.id }
        }.toMap()

        return identifiers.mapNotNull {
            val fhirId = identifierToFhirIdMap[Identifier(system = Uri(it.value.system), value = it.value.value)]

            if (fhirId != null) {
                it.key to fhirId
            } else {
                null
            }
        }.toMap()
    }

    private fun queryPractitionerFHIRIds(
        tenantMnemonic: String,
        batch: List<SystemValue>
    ): GraphQLResponse<LimitedPractitionersFHIR> {
        val query = javaClass.getResource("/graphql/AidboxPractitionerFHIRIDsQuery.graphql")!!.readText()
        val parameters = mapOf(
            "tenant" to SystemValue(
                system = "http://projectronin.com/id/tenantId",
                value = tenantMnemonic
            ).queryString,
            "identifiers" to batch.joinToString(separator = ",") { it.queryString }
        )
        val response: GraphQLResponse<LimitedPractitionersFHIR> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.body()
            } catch (e: LogMarkingException) {
                logger.warn(e.logMarker) { "Encountered exception when requesting Practitioner FHIR IDs from Aidbox: ${e.message}" }
                respondToGraphQLException(e)
            }
        }
        return response
    }

    /**
     * This method finds all Practitioners in Aidbox for the tenant mnemonic, and returns a [Map].
     * Each [Map] key is the FHIR ID for a Practitioner in Aidbox and
     * each [Map] value is the list of [Identifier]s for that Practitioner.
     */
    fun getPractitionersByTenant(tenantMnemonic: String): Map<String, List<Identifier>> {
        logger.info { "Retrieving Practitioners for $tenantMnemonic" }
        val query = javaClass.getResource("/graphql/PractitionerListQuery.graphql")!!.readText()
        val parameters = mapOf("identifier" to "http://projectronin.com/id/tenantId|$tenantMnemonic")
        val response: GraphQLResponse<PractitionerList> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.body()
            } catch (e: LogMarkingException) {
                logger.warn(e.logMarker) {
                    "Exception occurred while retrieving Practitioners for $tenantMnemonic: ${e.message}"
                }
                respondToGraphQLException(e)
            }
        }
        response.errors?.let { return emptyMap() }
        val idMap = response.data?.practitionerList?.associate { it.id.value to it.identifier } ?: emptyMap()
        logger.info { "Completed retrieving Practitioners from Aidbox for $tenantMnemonic" }
        return idMap
    }

    /**
     * Fetches a [Practitioner] from Aidbox for the [tenantMnemonic] and [practitionerFHIRID]
     */
    fun getPractitioner(tenantMnemonic: String, practitionerFHIRID: String): Practitioner {
        val practitioner = runBlocking<Practitioner> {
            val httpResponse = aidboxClient.getResource("Practitioner", practitionerFHIRID)
            httpResponse.body()
        }

        validateTenantIdentifier(
            tenantMnemonic,
            practitioner.identifier,
            "Tenant $tenantMnemonic cannot access practitioner $practitionerFHIRID"
        )

        return practitioner
    }
}

data class LimitedPractitioner(@JsonProperty("PractitionerList") val practitionerList: List<LimitedPractitionerIdentifiers>?)
data class LimitedPractitionerIdentifiers(val identifier: List<Identifier>)

data class PractitionerList(@JsonProperty("PractitionerList") val practitionerList: List<PartialPractitioner>?)
data class PartialPractitioner(val identifier: List<Identifier>, val id: Id)

data class LimitedPractitionersFHIR(@JsonProperty("PractitionerList") val practitionerList: List<LimitedPractitionerFHIRIdentifiers>?)
data class LimitedPractitionerFHIRIdentifiers(
    val id: String,
    @JsonProperty("identifier") val identifiers: List<Identifier>
)
