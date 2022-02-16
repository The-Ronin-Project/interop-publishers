package com.projectronin.interop.aidbox

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.utils.respondToGraphQLException
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import io.ktor.client.call.receive
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class PractitionerService(private val aidboxClient: AidboxClient) {
    private val logger = KotlinLogging.logger { }
    /**
     * Returns a [List] of [Identifier] for a single practitioner from AidBox.
     * @param practitionerFHIRID the FHIR ID of the practitioner (the key value in AidBox)
     */
    fun getPractitionerIdentifiers(practitionerFHIRID: String): List<Identifier> {
        logger.info { "Retrieving Practitioner Identifiers from Aidbox using FHIR ID" }
        val query = javaClass.getResource("/graphql/AidboxLimitedPractitionerIDsQuery.graphql")!!.readText()
        val parameters = mapOf("id" to practitionerFHIRID)
        val response: GraphQLResponse<LimitedPractitioner> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.receive()
            } catch (e: Exception) {
                logger.warn(e) {
                    "Encountered exception when requesting Practitioner Identifiers from Aidbox using FHIR ID"
                }
                respondToGraphQLException(e)
            }
        }
        response.errors?.let {
            logger.error {
                "Encountered errors while requesting Practitioner Identifiers from Aidbox using FHIR ID: $it"
            }
            return listOf()
        }
        logger.info { "Completed retrieving Practitioner Identifiers from Aidbox using FHIR ID" }
        return response.data?.practitioner?.identifier ?: listOf()
    }

    /**
     * Returns a specific [Identifier] for a single practitioner from AidBox.
     * @param practitionerFHIRID the FHIR ID of the practitioner (the key value in AidBox)
     * @param idType is a [CodeableConcept] representing the Identifer to retrieve.
     *  (see CodeableConcepts in package com.projectronin.interop.fhir.r4)
     */
    fun getSpecificPractitionerIdentifier(practitionerFHIRID: String, idType: CodeableConcept): Identifier? {
        return getPractitionerIdentifiers(practitionerFHIRID).find { it.type == idType }
    }

    /**
     * Returns a [Map] of FHIR ID to [Identifier] for a [List] of practitioner FHIR ids from AidBox.
     * @param practitionerFHIRIDs a list of FHIR IDs to lookup
     * @return a [Map] where the FHIR ID is the key, and a [List] of [Identifier] is the value.
     */
    fun getPractitionersIdentifiers(practitionerFHIRIDs: List<String>): Map<String, List<Identifier>> {
        val idMap = mutableMapOf<String, List<Identifier>>()
        practitionerFHIRIDs.forEach {
            idMap[it] = getPractitionerIdentifiers(it)
        }
        return idMap
    }

    /**
     * Returns a [List] of [List] of practitioner [Identifier] for a [String] representing a given tenant mnemonic.
     * @param tenantMnemonic a tenant mnemonic
     * @return a [List] of [List] of practitioner [Identifier]
     */
    fun getPractitionersByTenant(tenantMnemonic: String): Map<String, List<Identifier>> {
        logger.info { "Retrieving Practitioners for $tenantMnemonic" }
        val query = javaClass.getResource("/graphql/PractitionerListQuery.graphql")!!.readText()
        val parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
        val response: GraphQLResponse<PractitionerList> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.receive()
            } catch (e: Exception) {
                logger.error(e) {
                    "Exception occurred while retrieving Practitioners for $tenantMnemonic"
                }
                respondToGraphQLException(e)
            }
        }
        response.errors?.let {
            logger.error {
                "Encounters errors while retrieving Practitioners for $tenantMnemonic: $it"
            }
            return emptyMap()
        }
        val idMap = response.data?.practitionerList?.associate { it.id.value to it.identifier } ?: emptyMap()
        logger.info { "Completed retrieving Practitioners from Aidbox for $tenantMnemonic" }
        return idMap
    }
}

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class) // fix awful issue where Json doesn't recognize 'Practitioner' when uppercase
data class LimitedPractitioner(val practitioner: LimitedPractitionerIDs)
data class LimitedPractitionerIDs(val identifier: List<Identifier>)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class PractitionerList(val practitionerList: List<PartialPractitioner>)
data class PartialPractitioner(val identifier: List<Identifier>, val id: Id)
