package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.aidbox.client.model.GraphQLResponse
import com.projectronin.interop.aidbox.model.AidboxPatientList
import com.projectronin.interop.aidbox.utils.respondToException
import com.projectronin.interop.aidbox.utils.roninTenantNamespace
import io.ktor.client.call.receive
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Service providing access to patients within Aidbox.
 */
@Component
class PatientService(private val aidboxClient: AidboxClient) {
    private val logger = KotlinLogging.logger { }
    private val tenantNamespace = roninTenantNamespace()
    private val patientQuery =
        PatientService::class.java.getResource("/graphql/FindPatient.graphql").readText()

    /**
     * Finds the List of patients associated to the requested data.
     * @param tenantId The tenant in which to search for the patient.
     * @param birthDate The patient's date of birth.
     * @param givenName The patient's given name.
     * @param familyName The patient's family name.
     * @param aidboxAuthString The authorization string required for Aidbox.
     * @return A [GraphQLResponse] containing the [AidboxPatientList] of patients matching the search criteria.
     */
    fun findPatient(
        tenantId: String,
        birthDate: String,
        givenName: String,
        familyName: String,
        aidboxAuthString: String
    ): GraphQLResponse<AidboxPatientList> {
        logger.debug { "Retrieving patient list" }
        val parameters = mapOf(
            "tenant" to "$tenantNamespace|$tenantId",
            "birthDate" to birthDate,
            "givenName" to givenName,
            "familyName" to familyName
        )

        val graphQLResponse = runBlocking {
            try {
                val httpResponse = aidboxClient.query(patientQuery, aidboxAuthString, parameters)
                httpResponse.receive<GraphQLResponse<AidboxPatientList>>()
            } catch (e: Exception) {
                respondToException<AidboxPatientList>(e)
            }
        }

        // Even though we're returning the entire response, we're going to ensure all errors are logged out here.
        graphQLResponse.errors?.let {
            logger.error { "Encountered errors while requesting PatientList from Aidbox: $it" }
        }

        return graphQLResponse
    }
}
