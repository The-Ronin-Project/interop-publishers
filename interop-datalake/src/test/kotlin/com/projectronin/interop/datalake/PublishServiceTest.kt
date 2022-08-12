package com.projectronin.interop.datalake

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.datalake.azure.client.AzureClient
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Practitioner
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PublishServiceTest {
    private val mockClient = mockk<AzureClient> {}
    private val service = PublishService(mockClient)
    private val tenantId = "mockTenant"

    @Test
    fun `empty collection processes`() {
        assertTrue(service.publish(tenantId, emptyList()))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `real collection processes`() {
        mockkConstructor(LocalDate::class)
        mockkStatic(LocalDate::class)

        val mockkLocalDate = mockk<LocalDate> {
            every { format(any()) } returns "1990-01-03"
        }
        every { LocalDate.now() } returns mockkLocalDate // LocalDate.of(1990,1,3)

        val location1 = Location(
            id = Id("abc"),
            name = "Location1"
        )
        val location2 = Location(
            id = Id("def"),
            name = "Location2"
        )
        val practiconer = Practitioner(
            id = Id("abc"),
        )
        val filePathString = "/fhir-r4/__RESOURCETYPE__/date=1990-01-03/tenant_id=mockTenant/__FHIRID__.json"
        val locationFilePathString = filePathString.replace("__RESOURCETYPE__", "location")
        val practitionerFilePathString = filePathString.replace("__RESOURCETYPE__", "practitioner")
        val objectMapper = JacksonManager.objectMapper
        justRun { mockClient.upload(locationFilePathString.replace("__FHIRID__", "abc"), objectMapper.writeValueAsString(location1)) }
        justRun { mockClient.upload(locationFilePathString.replace("__FHIRID__", "def"), objectMapper.writeValueAsString(location2)) }
        justRun { mockClient.upload(practitionerFilePathString.replace("__FHIRID__", "abc"), objectMapper.writeValueAsString(practiconer)) }
        assertTrue(service.publish(tenantId, listOf(location1, location2, practiconer)))
        verify(exactly = 3) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `can publish a resource without an ID`() {
        val badResource = Location()
        assertThrows<IllegalStateException> { service.publish(tenantId, listOf(badResource)) }
    }

    @AfterEach
    fun unmockk() {
        unmockkAll()
    }
}
