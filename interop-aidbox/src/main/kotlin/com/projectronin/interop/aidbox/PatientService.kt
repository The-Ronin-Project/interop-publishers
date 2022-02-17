package com.projectronin.interop.aidbox

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.respondToGraphQLException
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import io.ktor.client.call.receive
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class PatientService(private val aidboxClient: AidboxClient) {
    private val logger = KotlinLogging.logger { }

    /**
     * Searches Aidbox for patients based on their [tenantMnemonic] and a map of keys to [SystemValue]s, each
     * representing a patient identifier.  Returns a map of the given keys, along with the patient's FHIR ID if it was
     * found, otherwise no entry for that key.
     */
    fun <K> getPatientFHIRIds(tenantMnemonic: String, identifiers: Map<K, SystemValue>): Map<K, String> {
        logger.info { "Retrieving Patient FHIR IDs from Aidbox" }

        val query = javaClass.getResource("/graphql/AidboxPatientFHIRIDsQuery.graphql")!!.readText()
        val parameters = mapOf(
            "tenant" to SystemValue(system = CodeSystem.RONIN_TENANT.uri.value, value = tenantMnemonic).queryString,
            "identifiers" to identifiers.values.joinToString(separator = ",") { it.queryString }
        )

        val response: GraphQLResponse<LimitedPatient> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.receive()
            } catch (e: Exception) {
                logger.warn(e) { "Encountered exception when requesting Patient FHIR IDs from Aidbox" }
                respondToGraphQLException(e)
            }
        }

        val identifierToFhirIdMap = response.data?.patientList?.flatMap {
            it.identifiers.map { identifier -> identifier to it.id }
        }?.toMap() ?: mapOf()

        return identifiers.mapNotNull {
            val fhirId = identifierToFhirIdMap[Identifier(system = Uri(it.value.system), value = it.value.value)]

            if (fhirId != null) {
                it.key to fhirId
            } else {
                null
            }
        }.toMap()
    }
}

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class LimitedPatient(val patientList: List<LimitedPatientIdentifiers>)
data class LimitedPatientIdentifiers(val id: String, @JsonProperty("identifier") val identifiers: List<Identifier>)
