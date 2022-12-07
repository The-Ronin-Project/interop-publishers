package com.projectronin.interop.aidbox.testcontainer.client

import com.projectronin.interop.aidbox.testcontainer.client.graphql.GraphQLError
import com.projectronin.interop.aidbox.testcontainer.client.graphql.GraphQLResponse
import com.projectronin.interop.aidbox.testcontainer.client.model.AidboxPatientList
import com.projectronin.interop.fhir.r4.CodeSystem
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

class PatientService(private val aidboxClient: AidboxClient) {
    private val patientQuery =
        PatientService::class.java.getResource("/graphql/FindPatient.graphql").readText()

    fun findPatient(
        tenantId: String,
        birthDate: String,
        givenName: String,
        familyName: String,
        aidboxAuthString: String,
    ): GraphQLResponse<AidboxPatientList> {
        val parameters = mapOf(
            "tenant" to "${CodeSystem.RONIN_TENANT.uri.value}|$tenantId",
            "birthDate" to birthDate,
            "givenName" to givenName,
            "familyName" to familyName
        )

        val graphQLResponse = runBlocking {
            try {
                val httpResponse = aidboxClient.query(patientQuery, aidboxAuthString, parameters)
                httpResponse.body<GraphQLResponse<AidboxPatientList>>()
            } catch (e: Exception) {
                respondToException<AidboxPatientList>(e)
            }
        }

        return graphQLResponse
    }

    suspend fun <T> respondToException(exception: Exception): GraphQLResponse<T> {
        val httpResponse = when (exception) {
            // This covers all of the types of responses handled by Ktor.
            is ResponseException -> exception.response
            else -> throw exception
        }

        val graphQLError =
            GraphQLError("Error communicating with Aidbox. Received status code ${httpResponse.status} with message \"${httpResponse.bodyAsText()}\"")
        return GraphQLResponse(errors = listOf(graphQLError))
    }
}
