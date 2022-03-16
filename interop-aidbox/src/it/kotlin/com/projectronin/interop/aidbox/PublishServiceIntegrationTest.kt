package com.projectronin.interop.aidbox

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.projectronin.interop.aidbox.spring.AidboxIntegrationConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.FHIRResource
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.LocationHoursOfOperation
import com.projectronin.interop.fhir.r4.datatype.LocationPosition
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitionerRole
import com.projectronin.interop.fhir.r4.ronin.resource.RoninResource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
class PublishServiceIntegrationTest : BaseAidboxTest() {
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

    @Autowired
    private lateinit var publishService: PublishService

    @Test
    fun `can publish a new resource`() {
        // Verify that the resource does not exist.
        val initialResource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-new-resource")
        assertNull(initialResource)

        val practitioner = OncologyPractitioner(
            id = Id("mdaoc-new-resource"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Smith", given = listOf("Josh")))
        )
        val published = publishService.publish(listOf(practitioner))
        assertTrue(published)

        val resource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-new-resource")
        assertEquals(practitioner, resource)
    }

    @Test
    @AidboxData("/aidbox/publish/PractitionerToUpdate.yaml")
    fun `can publish an updated resource`() {
        val practitioner = OncologyPractitioner(
            id = Id("mdaoc-existing-resource"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Doctor", given = listOf("Bob")))
        )

        // Verify that the resource does exist.
        val initialResource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-existing-resource")
        assertNotNull(initialResource)
        // And that it isn't what we are about to make it.
        assertNotEquals(practitioner, initialResource)

        val published = publishService.publish(listOf(practitioner))
        assertTrue(published)

        val resource = getResource<OncologyPractitioner>("Practitioner", "mdaoc-existing-resource")
        assertEquals(practitioner, resource)
    }

    @Test
    fun `can publish multiple resources of the same resourceType (both RoninResource)`() {
        // Before
        val idPrefix = "2P200-"
        val practitioner1 = OncologyPractitioner(
            id = Id("${idPrefix}cmjones"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "third"
                )
            ),
            name = listOf(HumanName(family = "Jones", given = listOf("Cordelia", "May")))
        )
        val practitioner2 = OncologyPractitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "second"
                )
            ),
            name = listOf(HumanName(family = "Llyr", given = listOf("Regan", "Anne")))
        )
        val testPractitioners = listOf(practitioner1, practitioner2)
        assertTrue(allRoninResourcesNull("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))

        // Test
        val published = publishService.publish(testPractitioners)
        assertTrue(published)
        assertTrue(allRoninResourcesExist("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))

        // After
        deleteAllResources("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr"))
    }

    @Test
    fun `can publish multiple resources of the same resourceType (both R4Resource)`() {
        // Before
        val idPrefix = "2L200-"
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id5"
                )
            ),
            mode = LocationMode.INSTANCE,
            status = LocationStatus.ACTIVE,
            name = "My Office",
            alias = listOf("Guest Room"),
            description = "Sun Room",
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic",
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(value = "8675309")),
            address = Address(country = "USA"),
            physicalType = CodeableConcept(
                text = "Room",
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132)),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    ),
                    allDay = true
                )
            ),
            availabilityExceptions = "Call for details"
        )
        val location2 = Location(
            id = Id("${idPrefix}12346"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id6"
                )
            ),
            mode = LocationMode.INSTANCE,
            status = LocationStatus.ACTIVE,
            name = "Back Study",
            alias = listOf("Studio"),
            description = "Game Room",
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic",
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(value = "123-456-7890")),
            address = Address(country = "USA"),
            physicalType = CodeableConcept(
                text = "Room",
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            hoursOfOperation = listOf(LocationHoursOfOperation(daysOfWeek = listOf(DayOfWeek.TUESDAY), allDay = true)),
            availabilityExceptions = "By appointment"
        )
        val testLocations = listOf(location1, location2)
        assertTrue(allR4ResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))

        // Test
        val published = publishService.publish(testLocations)
        assertTrue(published)
        assertTrue(allR4ResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))

        // After
        deleteAllResources("Location", listOf("${idPrefix}12345", "${idPrefix}12346"))
    }

    @Test
    fun `can publish multiple resources of different resourceTypes (both RoninResource)`() {
        val practitioner = OncologyPractitioner(
            id = Id("mdaoc-practitioner"),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "mdaoc"
                )
            ),
            name = listOf(HumanName(family = "Doctor", given = listOf("Bob")))
        )
        val patient = OncologyPatient(
            id = Id("mdaoc-patient"),
            identifier = listOf(
                Identifier(type = CodeableConcepts.RONIN_TENANT, system = CodeSystem.RONIN_TENANT.uri, value = "mdaoc"),
                Identifier(type = CodeableConcepts.MRN, system = CodeSystem.MRN.uri, value = "1234"),
                Identifier(
                    type = CodeableConcepts.FHIR_STU3_ID,
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    value = "patient"
                )
            ),
            name = listOf(HumanName(family = "Doe", given = listOf("John"))),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.EMAIL,
                    value = "john.doe@projectronin.com",
                    use = ContactPointUse.WORK
                )
            ),
            gender = AdministrativeGender.MALE,
            birthDate = Date("1976-02-16"),
            address = listOf(Address(text = "Address")),
            maritalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus"),
                        code = Code("U")
                    )
                ),
                text = "Unmarried"
            )
        )

        val published = publishService.publish(listOf(practitioner, patient))
        assertTrue(published)

        val resource1 = getResource<OncologyPractitioner>("Practitioner", "mdaoc-practitioner")
        assertEquals(practitioner, resource1)

        val resource2 = getResource<OncologyPatient>("Patient", "mdaoc-patient")
        assertEquals(patient, resource2)
    }

    @Test
    fun `can publish resource list with all referenced resources provided (RoninResource and R4Resource)`() {
        // Before
        val idPrefix = "2PRAllRef200-"
        val practitioner1 = OncologyPractitioner(
            id = Id("${idPrefix}cmjones"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "third"
                )
            ),
            name = listOf(HumanName(family = "Jones", given = listOf("Cordelia", "May")))
        )
        val practitioner2 = OncologyPractitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "second"
                )
            ),
            name = listOf(HumanName(family = "Llyr", given = listOf("Regan", "Anne")))
        )
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id5"
                )
            ),
            mode = LocationMode.INSTANCE,
            status = LocationStatus.ACTIVE,
            name = "My Office",
            alias = listOf("Guest Room"),
            description = "Sun Room",
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic",
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(value = "8675309")),
            address = Address(country = "USA"),
            physicalType = CodeableConcept(
                text = "Room",
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132)),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    ),
                    allDay = true
                )
            ),
            availabilityExceptions = "Call for details"
        )
        val location2 = Location(
            id = Id("${idPrefix}12346"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id6"
                )
            ),
            mode = LocationMode.INSTANCE,
            status = LocationStatus.ACTIVE,
            name = "Back Study",
            alias = listOf("Studio"),
            description = "Game Room",
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic",
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(value = "123-456-7890")),
            address = Address(country = "USA"),
            physicalType = CodeableConcept(
                text = "Room",
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            hoursOfOperation = listOf(LocationHoursOfOperation(daysOfWeek = listOf(DayOfWeek.TUESDAY), allDay = true)),
            availabilityExceptions = "By appointment"
        )
        val practitionerRole1 = OncologyPractitionerRole(
            id = Id("${idPrefix}12347"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id3"
                )
            ),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}cmjones"),
            location = listOf(Reference(reference = "Location/${idPrefix}12345")),
            availableTime = listOf(AvailableTime(allDay = false)),
            notAvailable = listOf(NotAvailable(description = "Not available now")),
            availabilityExceptions = "exceptions"
        )
        val practitionerRole2 = OncologyPractitionerRole(
            id = Id("${idPrefix}12348"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id4"
                )
            ),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}rallyr"),
            location = listOf(Reference(reference = "Location/${idPrefix}12346")),
            availableTime = listOf(AvailableTime(allDay = true)),
            notAvailable = listOf(NotAvailable(description = "Available now")),
            availabilityExceptions = "No exceptions"
        )
        val fullRoles: List<FHIRResource> = listOf(
            location1,
            location2,
            practitioner1,
            practitioner2,
            practitionerRole1,
            practitionerRole2
        )
        assertTrue(allR4ResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(allRoninResourcesNull("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(allRoninResourcesNull("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))

        // Test
        val published = publishService.publish(fullRoles)
        assertTrue(published)
        assertTrue(allR4ResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(allRoninResourcesExist("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(
            allRoninResourcesExist(
                "PractitionerRole",
                listOf("${idPrefix}12347", "${idPrefix}12348")
            )
        )

        // After
        deleteAllResources("Location", listOf("${idPrefix}12345", "${idPrefix}12346"))
        deleteAllResources("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr"))
        deleteAllResources("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348"))
    }

    @Test
    fun `can publish resource list with all referenced resources provided, plus an unreferenced FHIRResource (RoninResource and R4Resource)`() {
        // Before
        val idPrefix = "2PRAllRefPlusUnrelatedP200-"
        val practitioner1 = OncologyPractitioner(
            id = Id("${idPrefix}cmjones"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "third"
                )
            ),
            name = listOf(HumanName(family = "Jones", given = listOf("Cordelia", "May")))
        )
        val practitioner2 = OncologyPractitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "second"
                )
            ),
            name = listOf(HumanName(family = "Llyr", given = listOf("Regan", "Anne")))
        )
        val practitioner3 = OncologyPractitioner(
            id = Id("${idPrefix}gwalsh"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "first"
                )
            ),
            name = listOf(HumanName(family = "Walsh", given = listOf("Goneril")))
        )
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id5"
                )
            ),
            mode = LocationMode.INSTANCE,
            status = LocationStatus.ACTIVE,
            name = "My Office",
            alias = listOf("Guest Room"),
            description = "Sun Room",
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic",
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(value = "8675309")),
            address = Address(country = "USA"),
            physicalType = CodeableConcept(
                text = "Room",
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132)),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    ),
                    allDay = true
                )
            ),
            availabilityExceptions = "Call for details"
        )
        val location2 = Location(
            id = Id("${idPrefix}12346"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id6"
                )
            ),
            mode = LocationMode.INSTANCE,
            status = LocationStatus.ACTIVE,
            name = "Back Study",
            alias = listOf("Studio"),
            description = "Game Room",
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic",
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(value = "123-456-7890")),
            address = Address(country = "USA"),
            physicalType = CodeableConcept(
                text = "Room",
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            hoursOfOperation = listOf(LocationHoursOfOperation(daysOfWeek = listOf(DayOfWeek.TUESDAY), allDay = true)),
            availabilityExceptions = "By appointment"
        )
        val practitionerRole1 = OncologyPractitionerRole(
            id = Id("${idPrefix}12347"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id3"
                )
            ),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}cmjones"),
            location = listOf(Reference(reference = "Location/${idPrefix}12345")),
            availableTime = listOf(AvailableTime(allDay = false)),
            notAvailable = listOf(NotAvailable(description = "Not available now")),
            availabilityExceptions = "exceptions"
        )
        val practitionerRole2 = OncologyPractitionerRole(
            id = Id("${idPrefix}12348"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id4"
                )
            ),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}rallyr"),
            location = listOf(Reference(reference = "Location/${idPrefix}12346")),
            availableTime = listOf(AvailableTime(allDay = true)),
            notAvailable = listOf(NotAvailable(description = "Available now")),
            availabilityExceptions = "No exceptions"
        )
        val unrelatedResourceInList: List<FHIRResource> = listOf(
            location1,
            location2,
            practitioner1,
            practitioner2,
            practitioner3,
            practitionerRole1,
            practitionerRole2

        )
        assertTrue(allR4ResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(
            allRoninResourcesNull(
                "Practitioner",
                listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh")
            )
        )
        assertTrue(allRoninResourcesNull("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))

        // Test
        val published = publishService.publish(unrelatedResourceInList)
        assertTrue(published)
        assertTrue(allR4ResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(
            allRoninResourcesExist(
                "Practitioner",
                listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh")
            )
        )
        assertTrue(
            allRoninResourcesExist(
                "PractitionerRole",
                listOf("${idPrefix}12347", "${idPrefix}12348")
            )
        )

        // After
        deleteAllResources("Location", listOf("${idPrefix}12345", "${idPrefix}12346"))
        deleteAllResources("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh"))
        deleteAllResources("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348"))
    }

    @Test
    fun `cannot publish list of PractitionerRole if even one reference in one PractitionerRole cannot be resolved`() {
        // Before
        val idPrefix = "2PRNoRef400-"
        val practitioner1 = OncologyPractitioner(
            id = Id("${idPrefix}cmjones"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "third"
                )
            ),
            name = listOf(HumanName(family = "Jones", given = listOf("Cordelia", "May")))
        )
        val practitioner2 = OncologyPractitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "second"
                )
            ),
            name = listOf(HumanName(family = "Llyr", given = listOf("Regan", "Anne")))
        )
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED, div = "div"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id5"
                )
            ),
            mode = LocationMode.INSTANCE,
            status = LocationStatus.ACTIVE,
            name = "My Office",
            alias = listOf("Guest Room"),
            description = "Sun Room",
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic",
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(value = "8675309")),
            address = Address(country = "USA"),
            physicalType = CodeableConcept(
                text = "Room",
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132)),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    ),
                    allDay = true
                )
            ),
            availabilityExceptions = "Call for details"
        )
        val practitionerRole1 = OncologyPractitionerRole(
            id = Id("${idPrefix}12347"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id3"
                )
            ),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}cmjones"),
            location = listOf(Reference(reference = "Location/${idPrefix}12345")),
            availableTime = listOf(AvailableTime(allDay = false)),
            notAvailable = listOf(NotAvailable(description = "Not available now")),
            availabilityExceptions = "exceptions"
        )
        val practitionerRole2 = OncologyPractitionerRole(
            id = Id("${idPrefix}12348"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "id4"
                )
            ),
            active = true,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}rallyr"),
            location = listOf(Reference(reference = "Location/${idPrefix}12346")),
            availableTime = listOf(AvailableTime(allDay = true)),
            notAvailable = listOf(NotAvailable(description = "Available now")),
            availabilityExceptions = "No exceptions"
        )
        val practitionerRoles: List<FHIRResource> = listOf(
            practitioner1,
            practitioner2,
            location1,
            practitionerRole1,
            practitionerRole2
        )
        assertTrue(allRoninResourcesNull("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))

        // Test
        val published = publishService.publish(practitionerRoles)
        assertFalse(published)
        assertTrue(allRoninResourcesNull("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))
    }

    @Test
    fun `empty list of resources does not error`() {
        val collection = listOf<FHIRResource>()
        val published = publishService.publish(collection)
        assertTrue(published)
    }

    private inline fun <reified T : FHIRResource> getResource(resourceType: String, id: String): T? {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val response = try {
                aidbox.ktorClient.get<HttpResponse>("$aidboxUrl/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
                e.response
            }

            if (response.status.isSuccess()) {
                val objectNode = response.receive<ObjectNode>()
                // Remove meta since Aidbox sets it.
                objectNode.remove("meta")

                objectMapper.convertValue<T>(objectNode)
            } else if (response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw IllegalStateException("Error while purging test data: ${response.receive<String>()}")
            }
        }
    }

    private inline fun <reified T : RoninResource> getRoninResource(resourceType: String, id: String): T? {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val response = try {
                aidbox.ktorClient.get<HttpResponse>("$aidboxUrl/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
                e.response
            }

            if (response.status.isSuccess()) {
                val objectNode = response.receive<ObjectNode>()
                // Remove meta since Aidbox sets it.
                objectNode.remove("meta")

                objectMapper.convertValue<T>(objectNode)
            } else {
                null
            }
        }
    }

    private inline fun <reified T : Resource> getR4Resource(resourceType: String, id: String): T? {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val response = try {
                aidbox.ktorClient.get<HttpResponse>("$aidboxUrl/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
                e.response
            }

            if (response.status.isSuccess()) {
                val objectNode = response.receive<ObjectNode>()
                // Remove meta since Aidbox sets it.
                objectNode.remove("meta")

                objectMapper.convertValue<T>(objectNode)
            } else {
                null
            }
        }
    }

    private fun deleteResource(resourceType: String, id: String) {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            try {
                aidbox.ktorClient.delete<HttpResponse>("$aidboxUrl/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
            } // May examine exception or response if needed, not important to existing tests
        }
    }

    private fun deleteAllResources(resourceType: String, ids: List<String>) {
        ids.forEach {
            deleteResource(resourceType, it)
        }
    }

    private fun allRoninResourcesExist(resourceType: String, ids: List<String>): Boolean {
        ids.forEach {
            if (getRoninResource<RoninResource>(resourceType, it) == null) {
                return false
            }
        }
        return true
    }

    private fun allRoninResourcesNull(resourceType: String, ids: List<String>): Boolean {
        ids.forEach {
            if (getRoninResource<RoninResource>(resourceType, it) != null) {
                return false
            }
        }
        return true
    }

    private fun allR4ResourcesExist(resourceType: String, ids: List<String>): Boolean {
        ids.forEach {
            if (getR4Resource<Resource>(resourceType, it) == null) {
                return false
            }
        }
        return true
    }

    private fun allR4ResourcesNull(resourceType: String, ids: List<String>): Boolean {
        ids.forEach {
            if (getR4Resource<Resource>(resourceType, it) != null) {
                return false
            }
        }
        return true
    }
}
