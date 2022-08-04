package com.projectronin.interop.aidbox

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.utils.respondToGraphQLException
import com.projectronin.interop.aidbox.utils.validateTenantIdentifier
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PatientService(
    private val aidboxClient: AidboxClient,
    @Value("\${aidbox.batchSize:100}") private val batchSize: Int
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Searches Aidbox for patients based on their [tenantMnemonic] and a map of keys to [SystemValue]s, each
     * representing a patient identifier.  Returns a map of the given keys, along with the patient's FHIR ID if it was
     * found, otherwise no entry for that key.
     */
    fun <K> getPatientFHIRIds(tenantMnemonic: String, identifiers: Map<K, SystemValue>): Map<K, String> {
        logger.info { "Retrieving Patient FHIR IDs from Aidbox" }

        // Split out the identifiers into batches.
        val collectedPatients = identifiers.values.chunked(batchSize).flatMap { batch ->
            queryForPatientFHIRIds(tenantMnemonic, batch).data?.patientList ?: emptyList()
        }

        val identifierToFhirIdMap = collectedPatients.flatMap {
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

    private fun queryForPatientFHIRIds(
        tenantMnemonic: String,
        batch: List<SystemValue>
    ): GraphQLResponse<LimitedPatient> {
        val query = javaClass.getResource("/graphql/AidboxPatientFHIRIDsQuery.graphql")!!.readText()
        val parameters = mapOf(
            "tenant" to SystemValue(
                system = CodeSystem.RONIN_TENANT.uri.value,
                value = tenantMnemonic
            ).queryString,
            "identifiers" to batch.joinToString(separator = ",") { it.queryString }
        )

        val response: GraphQLResponse<LimitedPatient> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.body()
            } catch (e: Exception) {
                logger.warn(e) { "Encountered exception when requesting Patient FHIR IDs from Aidbox" }
                respondToGraphQLException(e)
            }
        }
        return response
    }

    /**
     * This method finds all Patients in Aidbox for the tenant mnemonic, and returns a [Map].
     * Each [Map] key is the FHIR ID for a Patient in Aidbox and
     * each [Map] value is the list of [Identifier]s for that Patient.
     */
    fun getPatientsByTenant(tenantMnemonic: String): Map<String, List<Identifier>> {
        logger.info { "Retrieving Patients for $tenantMnemonic" }
        val query = javaClass.getResource("/graphql/PatientListQuery.graphql")!!.readText()
        val parameters = mapOf("identifier" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantMnemonic")
        val response: GraphQLResponse<PatientList> = runBlocking {
            try {
                val httpResponse = aidboxClient.queryGraphQL(query, parameters)
                httpResponse.body()
            } catch (e: Exception) {
                logger.error(e) {
                    "Exception occurred while retrieving Patients for $tenantMnemonic"
                }
                respondToGraphQLException(e)
            }
        }
        response.errors?.let {
            logger.error {
                "Encounters errors while retrieving Patients for $tenantMnemonic: $it"
            }
            return emptyMap()
        }
        val idMap = response.data?.patientList?.associate { it.id.value to it.identifier } ?: emptyMap()
        logger.info { "Completed retrieving Patients from Aidbox for $tenantMnemonic" }
        return idMap
    }

    /**
     * Returns a [List] of all patient FHIR IDs in Aidbox for the tenant mnemonic.
     */
    fun getPatientFHIRIdsByTenant(tenantMnemonic: String): List<String> {
        return getPatientsByTenant(tenantMnemonic).keys.toList()
    }

    /**
     * Fetches an OncologyPatient object from Aidbox based on the Patient's FHIR ID.
     * @param tenantMnemonic the mnemonic of the tenant represented by this call.
     * @param patientFHIRID [String] the patient's FHIR ID.
     * @return [Patient]
     */
    fun getOncologyPatient(tenantMnemonic: String, patientFHIRID: String): Patient {
        val oncologyPatient: Patient = runBlocking {
            val httpResponse = aidboxClient.getResource("Patient", patientFHIRID)
            httpResponse.body()
        }

        validateTenantIdentifier(
            tenantMnemonic,
            oncologyPatient.identifier,
            "Tenant $tenantMnemonic cannot access patient $patientFHIRID"
        )

        return oncologyPatient
    }
}

@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class LimitedPatient(val patientList: List<LimitedPatientIdentifiers>?)
data class LimitedPatientIdentifiers(val id: String, @JsonProperty("identifier") val identifiers: List<Identifier>)

data class PatientList(@JsonProperty("PatientList") val patientList: List<PartialPatient>?)
data class PartialPatient(val identifier: List<Identifier>, val id: Id)
