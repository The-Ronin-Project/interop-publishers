package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.Resource
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublishServiceTest {
    private val collection = listOf(
        Practitioner(
            id = Id("cmjones"),
            identifier = listOf(
                Identifier(system = CodeSystem.NPI.uri, value = "third")
            ),
            name = listOf(
                HumanName(family = "Jones", given = listOf("Cordelia", "May"))
            )
        ),
        Practitioner(
            id = Id("rallyr"),
            identifier = listOf(
                Identifier(system = CodeSystem.NPI.uri, value = "second")
            ),
            name = listOf(
                HumanName(
                    family = "Llyr", given = listOf("Regan", "Anne")
                )
            )
        )
    )
    private val aidboxClient = mockk<AidboxClient>()
    private val publishService = PublishService(aidboxClient)

    @Test
    fun `publish list of 2 Practitioner resources to aidbox, response 200 true`() {
        val expectedSuccess = true
        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.status } returns HttpStatusCode.OK
        coEvery { aidboxClient.batchUpsert(collection) } returns httpResponse
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(collection)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `empty list, return true`() {
        val httpResponse = mockk<HttpResponse>()
        val collection = listOf<Resource<*>>()
        coEvery { httpResponse.status } returns HttpStatusCode.OK
        coEvery { aidboxClient.batchUpsert(collection) } returns httpResponse
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(collection)
        }
        assertTrue(actualSuccess)
    }

    @Test
    fun `publish list of 2 Practitioner resources to aidbox, response 1xx (or 2xx) false`() {
        val expectedSuccess = false
        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.status } returns HttpStatusCode.Continue
        coEvery { aidboxClient.batchUpsert(collection) } returns httpResponse
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(collection)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }

    @Test
    fun `publish list of 2 Practitioner resources to aidbox, exception 5xx (or 3xx or 4xx) false`() {
        val expectedSuccess = false
        val httpResponse = mockk<HttpResponse>()
        coEvery { httpResponse.status } returns HttpStatusCode.ServiceUnavailable
        coEvery { aidboxClient.batchUpsert(collection) } returns httpResponse
        val actualSuccess: Boolean = runBlocking {
            publishService.publish(collection)
        }
        assertEquals(actualSuccess, expectedSuccess)
    }
}
