package com.projectronin.interop.aidbox.change

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.util.asCode
import io.ktor.client.call.body
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class ChangeDetectionServiceTest {
    private lateinit var aidboxClient: AidboxClient
    private lateinit var service: ChangeDetectionService

    @BeforeEach
    fun setup() {
        aidboxClient = mockk()
        service = ChangeDetectionService(aidboxClient)
    }

    @Test
    fun `filter works for resource with no current hash`() {
        val location = Location(
            id = Id("location1")
        )

        val filtered = service.filterChangedResources(listOf(location))
        assertEquals(listOf(location), filtered)
    }

    @Test
    fun `filter works for resource with hash different than current hash`() {
        val location = Location(
            id = Id("location1")
        )
        val locationHash = location.hashCode() + 100
        addHashes(mapOf("Location" to mapOf("location1" to locationHash)))

        val filtered = service.filterChangedResources(listOf(location))
        assertEquals(listOf(location), filtered)
    }

    @Test
    fun `filter works for resource with same hash and actual resource matches`() {
        val location = Location(
            id = Id("location1")
        )
        val locationHash = location.hashCode()
        addHashes(mapOf("Location" to mapOf("location1" to locationHash)))

        val aidboxLocation = Location(
            id = Id("location1"),
            meta = Meta(profile = listOf(Canonical("profile1")))
        )
        coEvery { aidboxClient.getResource("Location", "location1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxLocation
        }

        val filtered = service.filterChangedResources(listOf(location))
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `filter works for resource with same hash and actual resource does not match`() {
        val location = Location(
            id = Id("location1")
        )
        val locationHash = location.hashCode()
        addHashes(mapOf("Location" to mapOf("location1" to locationHash)))

        val aidboxLocation = Location(
            id = Id("location1"),
            meta = Meta(profile = listOf(Canonical("profile1"))),
            name = FHIRString("Aidbox Location")
        )
        coEvery { aidboxClient.getResource("Location", "location1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxLocation
        }

        val filtered = service.filterChangedResources(listOf(location))
        assertEquals(listOf(location), filtered)
    }

    @Test
    fun `filter supports multiple resource types`() {
        val location = Location(
            id = Id("location1")
        )
        val locationHash = location.hashCode()

        val patient = Patient(
            id = Id("patient1")
        )
        val patientHash = patient.hashCode()
        addHashes(
            mapOf(
                "Location" to mapOf("location1" to locationHash),
                "Patient" to mapOf("patient1" to patientHash)
            )
        )

        val aidboxLocation = Location(
            id = Id("location1"),
            meta = Meta(profile = listOf(Canonical("profile1"))),
            name = FHIRString("Aidbox Location")
        )
        coEvery { aidboxClient.getResource("Location", "location1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxLocation
        }

        val aidboxPatient = Patient(
            id = Id("patient1"),
            meta = Meta(profile = listOf(Canonical("profile1"))),
            gender = AdministrativeGender.MALE.asCode()
        )
        coEvery { aidboxClient.getResource("Patient", "patient1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxPatient
        }

        val filtered = service.filterChangedResources(listOf(location, patient))
        assertEquals(listOf(location, patient), filtered)
    }

    @Test
    fun `filters out some resources`() {
        val location = Location(
            id = Id("location1")
        )
        val locationHash = location.hashCode()

        val patient = Patient(
            id = Id("patient1")
        )
        val patientHash = patient.hashCode()
        addHashes(
            mapOf(
                "Location" to mapOf("location1" to locationHash),
                "Patient" to mapOf("patient1" to patientHash)
            )
        )

        val aidboxLocation = Location(
            id = Id("location1"),
            meta = Meta(profile = listOf(Canonical("profile1"))),
            name = FHIRString("Aidbox Location")
        )
        coEvery { aidboxClient.getResource("Location", "location1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxLocation
        }

        val aidboxPatient = Patient(
            id = Id("patient1"),
            meta = Meta(profile = listOf(Canonical("profile1")))
        )
        coEvery { aidboxClient.getResource("Patient", "patient1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxPatient
        }

        val filtered = service.filterChangedResources(listOf(location, patient))
        assertEquals(listOf(location), filtered)
    }

    @Test
    fun `filters out all resources`() {
        val location = Location(
            id = Id("location1")
        )
        val locationHash = location.hashCode()

        val patient = Patient(
            id = Id("patient1")
        )
        val patientHash = patient.hashCode()
        addHashes(
            mapOf(
                "Location" to mapOf("location1" to locationHash),
                "Patient" to mapOf("patient1" to patientHash)
            )
        )

        val aidboxLocation = Location(
            id = Id("location1"),
            meta = Meta(profile = listOf(Canonical("profile1")))
        )
        coEvery { aidboxClient.getResource("Location", "location1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxLocation
        }

        val aidboxPatient = Patient(
            id = Id("patient1"),
            meta = Meta(profile = listOf(Canonical("profile1")))
        )
        coEvery { aidboxClient.getResource("Patient", "patient1") } returns mockk {
            coEvery { body<Resource<*>>() } returns aidboxPatient
        }

        val filtered = service.filterChangedResources(listOf(location, patient))
        assertTrue(filtered.isEmpty())
    }

    private fun addHashes(hashesByType: Map<String, Map<String, Int>>) {
        val property = service.javaClass.kotlin.memberProperties.find { it.name == "resourceHashesByType" }!!
        property.isAccessible = true

        val current = property.get(service) as MutableMap<String, MutableMap<String, Int>>
        hashesByType.forEach { (type, hashes) ->
            val currentHashes = current.computeIfAbsent(type) { mutableMapOf() }
            hashes.forEach { (id, hash) ->
                currentHashes[id] = hash
            }
        }
    }
}
