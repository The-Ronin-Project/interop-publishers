package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import datadog.trace.api.Trace
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class ConceptMapService(private val aidboxClient: AidboxClient) {
    /**
     * Fetches a [ConceptMap] from Aidbox
     */
    @Trace
    fun getConceptMap(conceptMapFHIRID: String): ConceptMap {
        return runBlocking {
            aidboxClient.getResource("ConceptMap", conceptMapFHIRID).body()
        }
    }
}
