package com.projectronin.interop.aidbox

import com.projectronin.interop.aidbox.model.SystemValue
import com.projectronin.interop.aidbox.spring.AidboxIntegrationConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [AidboxIntegrationConfig::class])
@AidboxData("/aidbox/location/Locations.yaml")
class LocationServiceIntegrationTest : BaseAidboxTest() {
    @Autowired
    private lateinit var locationService: LocationService
    private val epicDepartmentSystem = "urn:oid:1.2.840.114350.1.13.297.3.7.2.686980"
    private val unknownSystem = "kruskal"

    companion object {
        // allows us to dynamically change the aidbox port to the testcontainer instance
        @JvmStatic
        @DynamicPropertySource
        fun aidboxUrlProperties(registry: DynamicPropertyRegistry) {
            registry.add("aidbox.url") { aidbox.baseUrl() }
            registry.add("aidbox.client.id") { aidbox.aidboxClientId }
            registry.add("aidbox.client.secret") { aidbox.aidboxClientSecret }
        }
    }

    @Test
    fun `no location FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("503161", epicDepartmentSystem),
            2 to SystemValue("5031610089", unknownSystem)
        )
        val fhirIds = locationService.getLocationFHIRIds("ronin", identifiers)
        assertTrue(fhirIds.isEmpty())
    }

    @Test
    fun `some location FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("5031610089", epicDepartmentSystem),
            2 to SystemValue("5031610089", unknownSystem)
        )
        val fhirIds = locationService.getLocationFHIRIds("ronin", identifiers)
        assertEquals(1, fhirIds.size)
        assertEquals("ronin-e1O7rbysoLddQsbsvfjKJvLOvBMDJwrf06OE6bKCBN4c3", fhirIds[1])
    }

    @Test
    fun `all location FHIR Ids found for identifiers`() {
        val identifiers = mapOf(
            1 to SystemValue("503548000", epicDepartmentSystem),
            2 to SystemValue("5031610089", epicDepartmentSystem),
            3 to SystemValue("5031610090", epicDepartmentSystem)
        )
        val fhirIds = locationService.getLocationFHIRIds("ronin", identifiers)
        assertEquals(3, fhirIds.size)
        assertEquals("ronin-e0nTqDB9tPOCaY4ODzeTeSWBZBT0YlpU8phB88YramOI3", fhirIds[1])
        assertEquals("ronin-e1O7rbysoLddQsbsvfjKJvLOvBMDJwrf06OE6bKCBN4c3", fhirIds[2])
        assertEquals("ronin-e3HhsZW6y9UW.W6TkIYuVk5kdFQV3gMSB.S-V6TTYxAI3", fhirIds[3])
    }

    @Test
    fun `all location identifers found for FHIR ID`() {
        val identifiers = listOf(
            SystemValue("e0nTqDB9tPOCaY4ODzeTeSWBZBT0YlpU8phB88YramOI3", "http://projectronin.com/id/fhir"),
            SystemValue("e1O7rbysoLddQsbsvfjKJvLOvBMDJwrf06OE6bKCBN4c3", "http://projectronin.com/id/fhir"),
            SystemValue("e3HhsZW6y9UW.W6TkIYuVk5kdFQV3gMSB.S-V6TTYxAI3", "http://projectronin.com/id/fhir")
        )
        val locationList = locationService.getAllLocationIdentifiers("ronin", identifiers)
        assertEquals(3, locationList.size)
    }

    @Test
    fun `identifiers are restricted to requested tenant when searching for location FHIR Ids`() {
        val identifiers = mapOf(
            1 to SystemValue("503548000", epicDepartmentSystem),
            2 to SystemValue("5031610089", epicDepartmentSystem),
            3 to SystemValue("5031610090", epicDepartmentSystem)
        )
        val fhirIds = locationService.getLocationFHIRIds("tenant", identifiers)
        assertTrue(fhirIds.isEmpty())
    }
}
