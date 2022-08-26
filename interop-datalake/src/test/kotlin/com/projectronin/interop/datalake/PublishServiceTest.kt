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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PublishServiceTest {
    private val mockClient = mockk<AzureClient> {}
    private val service = PublishService(mockClient)
    private val tenantId = "mockTenant"

    @Test
    fun `empty FHIR R4 collection processes`() {
        assertTrue(service.publishFHIRR4(tenantId, emptyList()))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `real FHIR R4 collection processes`() {
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
        val practitioner = Practitioner(
            id = Id("abc"),
        )
        val filePathString = "/fhir-r4/date=1990-01-03/tenant_id=mockTenant/resource_type=__RESOURCETYPE__/__FHIRID__.json"
        val locationFilePathString = filePathString.replace("__RESOURCETYPE__", "Location")
        val practitionerFilePathString = filePathString.replace("__RESOURCETYPE__", "Practitioner")
        val objectMapper = JacksonManager.objectMapper
        justRun { mockClient.upload(locationFilePathString.replace("__FHIRID__", "abc"), objectMapper.writeValueAsString(location1)) }
        justRun { mockClient.upload(locationFilePathString.replace("__FHIRID__", "def"), objectMapper.writeValueAsString(location2)) }
        justRun { mockClient.upload(practitionerFilePathString.replace("__FHIRID__", "abc"), objectMapper.writeValueAsString(practitioner)) }
        assertTrue(service.publishFHIRR4(tenantId, listOf(location1, location2, practitioner)))
        verify(exactly = 3) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `cannot publish a FHIR R4 resource that has no id`() {
        val badResource = Location()
        val exception = assertThrows<IllegalStateException> {
            service.publishFHIRR4(tenantId, listOf(badResource))
        }
        Assertions.assertEquals(
            "Attempted to publish a Location resource without a FHIR ID for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `cannot publish a FHIR R4 resource that has a null id`() {
        val badResource = Location(id = null)
        val exception = assertThrows<IllegalStateException> {
            service.publishFHIRR4(tenantId, listOf(badResource))
        }
        Assertions.assertEquals(
            "Attempted to publish a Location resource without a FHIR ID for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `empty API JSON data processes`() {
        assertTrue(service.publishAPIJSON(tenantId, "", "GET", "/fhir/Appointment"))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `empty API JSON method does not process`() {
        val exception = assertThrows<IllegalStateException> {
            service.publishAPIJSON(tenantId, "", "", "/fhir/Appointment")
        }
        Assertions.assertEquals(
            "Attempted to publish JSON data from an API response without identifying the API request for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `empty API JSON url does not process`() {
        val exception = assertThrows<IllegalStateException> {
            service.publishAPIJSON(tenantId, "", "GET", "")
        }
        Assertions.assertEquals(
            "Attempted to publish JSON data from an API response without identifying the API request for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `real API JSON processes`() {
        mockkConstructor(LocalDate::class)
        mockkStatic(LocalDate::class)
        val mockkLocalDate = mockk<LocalDate> {
            every { format(DateTimeFormatter.ISO_LOCAL_DATE) } returns "1990-01-03"
        }
        every { LocalDate.now() } returns mockkLocalDate

        mockkConstructor(LocalTime::class)
        mockkStatic(LocalTime::class)
        val mockkLocalTime = mockk<LocalTime> {
            every { format(DateTimeFormatter.ISO_LOCAL_TIME) } returns "06:07:42.999"
        }
        every { LocalTime.now() } returns mockkLocalTime

        val data = """
            {
                "first": "this",
                "second": {
                    "aaa": "that",
                    "bbb": "other"
                }
            }
        """.trimIndent()

        val filePathString = "/api-json/schema=GET-customAppointmentByPatient/date=1990-01-03/tenant_id=mockTenant/06-07-42-999.json"
        justRun { mockClient.upload(filePathString, data) }
        assertTrue(service.publishAPIJSON(tenantId, data, "GET", "/custom/AppointmentByPatient"))
        verify(exactly = 1) { mockClient.upload(any(), any()) }
    }

    @AfterEach
    fun unmockk() {
        unmockkAll()
    }
}
