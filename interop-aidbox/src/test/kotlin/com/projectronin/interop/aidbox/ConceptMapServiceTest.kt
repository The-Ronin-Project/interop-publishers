package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ConceptMapServiceTest {
    private val aidboxClient = mockk<AidboxClient>()
    private val mapService = ConceptMapService(aidboxClient)

    @Test
    fun `get concept map works`() {
        val map = mockk<ConceptMap>()
        val response = mockk<HttpResponse>()
        coEvery { aidboxClient.getResource("ConceptMap", "12345") } returns response
        coEvery<ConceptMap> { response.body() } returns map
        assertEquals(map, mapService.getConceptMap("12345"))
    }
}
