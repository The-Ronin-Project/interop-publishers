package com.projectronin.interop.aidbox

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.projectronin.interop.aidbox.spring.AidboxIntegrationConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.AvailableTime
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Communication
import com.projectronin.interop.fhir.r4.datatype.Contact
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.LocationHoursOfOperation
import com.projectronin.interop.fhir.r4.datatype.LocationPosition
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.NotAvailable
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.PatientLink
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ParticipationStatus
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
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
        val initialResource = getResource<Practitioner>("Practitioner", "mdaoc-new-resource")
        assertNull(initialResource)

        val practitioner = Practitioner(
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

        val resource = getResource<Practitioner>("Practitioner", "mdaoc-new-resource")
        assertEquals(practitioner, resource)
    }

    @Test
    @AidboxData("/aidbox/publish/PractitionerToUpdate.yaml")
    fun `can publish an updated resource`() {
        val practitioner = Practitioner(
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
        val initialResource = getResource<Practitioner>("Practitioner", "mdaoc-existing-resource")
        assertNotNull(initialResource)
        // And that it isn't what we are about to make it.
        assertNotEquals(practitioner, initialResource)

        val published = publishService.publish(listOf(practitioner))
        assertTrue(published)

        val resource = getResource<Practitioner>("Practitioner", "mdaoc-existing-resource")
        assertEquals(practitioner, resource)
    }

    @Test
    fun `can publish multiple resources of the same resourceType (both RoninResource)`() {
        // Before
        val idPrefix = "2P200-"
        val practitioner1 = Practitioner(
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
        val practitioner2 = Practitioner(
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
        assertTrue(allResourcesNull("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))

        // Test
        val published = publishService.publish(testPractitioners)
        assertTrue(published)
        assertTrue(allResourcesExist("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))

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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "123-456-7890")),
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
        assertTrue(allResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))

        // Test
        val published = publishService.publish(testLocations)
        assertTrue(published)
        assertTrue(allResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))

        // After
        deleteAllResources("Location", listOf("${idPrefix}12345", "${idPrefix}12346"))
    }

    @Test
    fun `can publish multiple resources of different resourceTypes (both RoninResource)`() {
        val practitioner = Practitioner(
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
        val patient = Patient(
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

        val resource1 = getResource<Practitioner>("Practitioner", "mdaoc-practitioner")
        assertEquals(practitioner, resource1)

        val resource2 = getResource<Patient>("Patient", "mdaoc-patient")
        assertEquals(patient, resource2)
    }

    @Test
    fun `can publish resources with all references provided (RoninResource, R4Resource) - PractitionerRole, Practitioner, Location`() {
        // Before
        val idPrefix = "2PRAllRef200-"
        val practitioner1 = Practitioner(
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
        val practitioner2 = Practitioner(
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "123-456-7890")),
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
        val practitionerRole1 = PractitionerRole(
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
        val practitionerRole2 = PractitionerRole(
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
        val fullRoles: List<Resource<*>> = listOf(
            location1,
            location2,
            practitioner1,
            practitioner2,
            practitionerRole1,
            practitionerRole2
        )
        assertTrue(allResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(allResourcesNull("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(allResourcesNull("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))

        // Test
        val published = publishService.publish(fullRoles)
        assertTrue(published)
        assertTrue(allResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(allResourcesExist("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(
            allResourcesExist(
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
    fun `can publish resources with all references provided (RoninResource) - Appointment, Practitioner, Patient`() {
        // Before
        val idPrefix = "1AptAllRef200-"
        val practitioner1 = Practitioner(
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
        val practitioner2 = Practitioner(
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
        val patient = Patient(
            id = Id("${idPrefix}12345"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
                Identifier(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID,
                    value = "fhirId"
                )
            ),
            active = true,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    value = "8675309",
                    use = ContactPointUse.MOBILE
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(type = DynamicValueType.BOOLEAN, value = false),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M"),
            multipleBirth = DynamicValue(type = DynamicValueType.INTEGER, value = 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(Contact(name = HumanName(text = "Jane Doe"))),
            communication = listOf(Communication(language = CodeableConcept(text = "English"))),
            generalPractitioner = listOf(Reference(reference = "Practitioner/${idPrefix}cmjones")),
            managingOrganization = Reference(display = "organization"),
            link = listOf(PatientLink(other = Reference(display = "Patient"), type = LinkType.REPLACES))
        )
        val appointment = Appointment(
            id = Id("${idPrefix}12345"),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            status = AppointmentStatus.CANCELLED,
            appointmentType = CodeableConcept(text = "appointment type"),
            cancelationReason = CodeableConcept(text = "cancel reason"),
            serviceCategory = listOf(CodeableConcept(text = "service category")),
            serviceType = listOf(CodeableConcept(text = "service type")),
            specialty = listOf(CodeableConcept(text = "specialty")),
            reasonCode = listOf(CodeableConcept(text = "reason code")),
            reasonReference = listOf(Reference(display = "reason reference")),
            priority = 1,
            description = "appointment test",
            start = Instant(value = "2017-01-01T00:00:00Z"),
            end = Instant(value = "2017-01-01T01:00:00Z"),
            minutesDuration = 15,
            slot = listOf(Reference(display = "slot")),
            created = DateTime(value = "2021-11-16"),
            comment = "comment",
            patientInstruction = "patient instruction",
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Patient/${idPrefix}12345"),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}cmjones"),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}rallyr"),
                    status = ParticipationStatus.DECLINED
                )
            ),
            requestedPeriod = listOf(
                Period(
                    start = DateTime(value = "2021-11-15"),
                    end = DateTime(value = "2021-11-17")
                )
            )
        )
        val fullAppointment: List<Resource<*>> = listOf(
            practitioner1,
            practitioner2,
            patient,
            appointment
        )
        assertTrue(allResourcesNull("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(allResourcesNull("Patient", listOf("${idPrefix}12345")))
        assertTrue(allResourcesNull("Appointment", listOf("${idPrefix}12345")))

        // Test
        val published = publishService.publish(fullAppointment)
        assertTrue(published)
        assertTrue(allResourcesExist("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(allResourcesExist("Patient", listOf("${idPrefix}12345")))
        assertTrue(allResourcesExist("Appointment", listOf("${idPrefix}12345")))

        // After
        deleteAllResources("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr"))
        deleteAllResources("Patient", listOf("${idPrefix}12345"))
        deleteAllResources("Appointment", listOf("${idPrefix}12345"))
    }

    @Test
    fun `can publish PractitionerRole with all references provided, plus an extra resource (RoninResource and R4Resource)`() {
        // Before
        val idPrefix = "2PRAllRefPlusUnrelatedP200-"
        val practitioner1 = Practitioner(
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
        val practitioner2 = Practitioner(
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
        val practitioner3 = Practitioner(
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "123-456-7890")),
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
        val practitionerRole1 = PractitionerRole(
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
        val practitionerRole2 = PractitionerRole(
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
        val unrelatedResourceInList: List<Resource<*>> = listOf(
            location1,
            location2,
            practitioner1,
            practitioner2,
            practitioner3,
            practitionerRole1,
            practitionerRole2

        )
        assertTrue(allResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(
            allResourcesNull(
                "Practitioner",
                listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh")
            )
        )
        assertTrue(allResourcesNull("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))

        // Test
        val published = publishService.publish(unrelatedResourceInList)
        assertTrue(published)
        assertTrue(allResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(
            allResourcesExist(
                "Practitioner",
                listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh")
            )
        )
        assertTrue(
            allResourcesExist(
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
    fun `can publish Appointment with all references provided, plus extra resources (RoninResource and R4Resource)`() {
        // Before
        val idPrefix = "1AptAllRefPlusUnrelatedP200-"
        val practitioner1 = Practitioner(
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
        val practitioner2 = Practitioner(
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
        val practitioner3 = Practitioner(
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "123-456-7890")),
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
        val patient = Patient(
            id = Id("${idPrefix}12345"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
                Identifier(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID,
                    value = "fhirId"
                )
            ),
            active = true,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    value = "8675309",
                    use = ContactPointUse.MOBILE
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(type = DynamicValueType.BOOLEAN, value = false),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M"),
            multipleBirth = DynamicValue(type = DynamicValueType.INTEGER, value = 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(Contact(name = HumanName(text = "Jane Doe"))),
            communication = listOf(Communication(language = CodeableConcept(text = "English"))),
            generalPractitioner = listOf(Reference(reference = "Practitioner/${idPrefix}cmjones")),
            managingOrganization = Reference(display = "organization"),
            link = listOf(PatientLink(other = Reference(display = "Patient"), type = LinkType.REPLACES))
        )
        val appointment = Appointment(
            id = Id("${idPrefix}12345"),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            status = AppointmentStatus.CANCELLED,
            appointmentType = CodeableConcept(text = "appointment type"),
            cancelationReason = CodeableConcept(text = "cancel reason"),
            serviceCategory = listOf(CodeableConcept(text = "service category")),
            serviceType = listOf(CodeableConcept(text = "service type")),
            specialty = listOf(CodeableConcept(text = "specialty")),
            reasonCode = listOf(CodeableConcept(text = "reason code")),
            reasonReference = listOf(Reference(display = "reason reference")),
            priority = 1,
            description = "appointment test",
            start = Instant(value = "2017-01-01T00:00:00Z"),
            end = Instant(value = "2017-01-01T01:00:00Z"),
            minutesDuration = 15,
            slot = listOf(Reference(display = "slot")),
            created = DateTime(value = "2021-11-16"),
            comment = "comment",
            patientInstruction = "patient instruction",
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Patient/${idPrefix}12345"),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}cmjones"),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}rallyr"),
                    status = ParticipationStatus.DECLINED
                )
            ),
            requestedPeriod = listOf(
                Period(
                    start = DateTime(value = "2021-11-15"),
                    end = DateTime(value = "2021-11-17")
                )
            )
        )
        val unrelatedResourceInList: List<Resource<*>> = listOf(
            location1,
            location2,
            practitioner1,
            practitioner2,
            practitioner3,
            patient,
            appointment
        )
        assertTrue(allResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(
            allResourcesNull(
                "Practitioner",
                listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh")
            )
        )
        assertTrue(allResourcesNull("Patient", listOf("${idPrefix}12345")))
        assertTrue(allResourcesNull("Appointment", listOf("${idPrefix}12345")))

        // Test
        val published = publishService.publish(unrelatedResourceInList)
        assertTrue(published)
        assertTrue(allResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))
        assertTrue(
            allResourcesExist(
                "Practitioner",
                listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh")
            )
        )
        assertTrue(allResourcesExist("Patient", listOf("${idPrefix}12345")))
        assertTrue(allResourcesExist("Appointment", listOf("${idPrefix}12345")))

        // After
        deleteAllResources("Location", listOf("${idPrefix}12345", "${idPrefix}12346"))
        deleteAllResources("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}gwalsh"))
        deleteAllResources("Patient", listOf("${idPrefix}12345"))
        deleteAllResources("Appointment", listOf("${idPrefix}12345"))
    }

    @Test
    fun `can publish list of PractitionerRole with reference in one PractitionerRole that cannot be resolved`() {
        // Before
        val idPrefix = "2PRNoRef400-"
        val practitioner1 = Practitioner(
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
        val practitioner2 = Practitioner(
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
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
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
        val practitionerRole1 = PractitionerRole(
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
        val practitionerRole2 = PractitionerRole(
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
        val practitionerRoles: List<Resource<*>> = listOf(
            practitioner1,
            practitioner2,
            location1,
            practitionerRole1,
            practitionerRole2
        )
        assertTrue(allResourcesNull("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(allResourcesNull("Location", listOf("${idPrefix}12345")))
        assertTrue(allResourcesNull("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))

        // Test
        val published = publishService.publish(practitionerRoles)
        assertTrue(published)
        assertTrue(allResourcesExist("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(allResourcesExist("Location", listOf("${idPrefix}12345")))
        assertTrue(allResourcesExist("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348")))

        deleteAllResources("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr"))
        deleteAllResources("Location", listOf("${idPrefix}12345"))
        deleteAllResources("PractitionerRole", listOf("${idPrefix}12347", "${idPrefix}12348"))
    }

    @Test
    fun `can publish Appointment with reference that cannot be resolved`() {
        // Before
        val idPrefix = "1AptNoRef400-"
        val practitioner1 = Practitioner(
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
        val practitioner2 = Practitioner(
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
        val patient = Patient(
            id = Id("${idPrefix}12345"),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                ),
                Identifier(
                    system = CodeSystem.MRN.uri,
                    type = CodeableConcepts.MRN,
                    value = "MRN"
                ),
                Identifier(
                    system = CodeSystem.FHIR_STU3_ID.uri,
                    type = CodeableConcepts.FHIR_STU3_ID,
                    value = "fhirId"
                )
            ),
            active = true,
            name = listOf(HumanName(family = "Doe")),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE,
                    value = "8675309",
                    use = ContactPointUse.MOBILE
                )
            ),
            gender = AdministrativeGender.FEMALE,
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(type = DynamicValueType.BOOLEAN, value = false),
            address = listOf(Address(country = "USA")),
            maritalStatus = CodeableConcept(text = "M"),
            multipleBirth = DynamicValue(type = DynamicValueType.INTEGER, value = 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(Contact(name = HumanName(text = "Jane Doe"))),
            communication = listOf(Communication(language = CodeableConcept(text = "English"))),
            generalPractitioner = listOf(Reference(reference = "Practitioner/${idPrefix}cmjones")),
            managingOrganization = Reference(display = "organization"),
            link = listOf(PatientLink(other = Reference(display = "Patient"), type = LinkType.REPLACES))
        )
        val appointment = Appointment(
            id = Id("${idPrefix}12345"),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.com/fhir/us/ronin/StructureDefinition/partnerDepartmentReference"),
                    value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "reference"))
                )
            ),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.RONIN_TENANT.uri,
                    type = CodeableConcepts.RONIN_TENANT,
                    value = "tenantId"
                )
            ),
            status = AppointmentStatus.CANCELLED,
            appointmentType = CodeableConcept(text = "appointment type"),
            cancelationReason = CodeableConcept(text = "cancel reason"),
            serviceCategory = listOf(CodeableConcept(text = "service category")),
            serviceType = listOf(CodeableConcept(text = "service type")),
            specialty = listOf(CodeableConcept(text = "specialty")),
            reasonCode = listOf(CodeableConcept(text = "reason code")),
            reasonReference = listOf(Reference(display = "reason reference")),
            priority = 1,
            description = "appointment test",
            start = Instant(value = "2017-01-01T00:00:00Z"),
            end = Instant(value = "2017-01-01T01:00:00Z"),
            minutesDuration = 15,
            slot = listOf(Reference(display = "slot")),
            created = DateTime(value = "2021-11-16"),
            comment = "comment",
            patientInstruction = "patient instruction",
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Patient/${idPrefix}12345"),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}12345"),
                    status = ParticipationStatus.ACCEPTED
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}rallyr"),
                    status = ParticipationStatus.DECLINED
                )
            ),
            requestedPeriod = listOf(
                Period(
                    start = DateTime(value = "2021-11-15"),
                    end = DateTime(value = "2021-11-17")
                )
            )
        )
        val resourceList: List<Resource<*>> = listOf(
            practitioner1,
            practitioner2,
            patient,
            appointment
        )
        assertTrue(
            allResourcesNull(
                "Practitioner",
                listOf("${idPrefix}cmjones", "${idPrefix}rallyr", "${idPrefix}12345")
            )
        )
        assertTrue(allResourcesNull("Patient", listOf("${idPrefix}12345")))
        assertTrue(allResourcesNull("Appointment", listOf("${idPrefix}12345")))

        // Test
        val published = publishService.publish(resourceList)
        assertTrue(published)
        assertTrue(allResourcesExist("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))
        assertTrue(allResourcesExist("Patient", listOf("${idPrefix}12345")))
        assertTrue(allResourcesExist("Appointment", listOf("${idPrefix}12345")))

        deleteAllResources("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr"))
        deleteAllResources("Patient", listOf("${idPrefix}12345"))
        deleteAllResources("Appointment", listOf("${idPrefix}12345"))
    }

    @Test
    fun `empty list of resources does not error`() {
        val collection = listOf<Resource<*>>()
        val published = publishService.publish(collection)
        assertTrue(published)
    }

    private inline fun <reified T : Resource<T>> getResource(resourceType: String, id: String): T? {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val response = try {
                aidbox.ktorClient.get("$aidboxUrl/fhir/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
                e.response
            }

            if (response.status.isSuccess()) {
                val objectNode = response.body<ObjectNode>()
                // Remove meta since Aidbox sets it.
                objectNode.remove("meta")

                objectMapper.convertValue<T>(objectNode)
            } else if (response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw IllegalStateException("Error while purging test data: ${response.bodyAsText()}")
            }
        }
    }

    private fun deleteResource(resourceType: String, id: String) {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            try {
                aidbox.ktorClient.delete("$aidboxUrl/fhir/$resourceType/$id") {
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

    private fun allResourcesExist(resourceType: String, ids: List<String>): Boolean {
        ids.forEach {
            if (!doesResourceExist(resourceType, it)) {
                return false
            }
        }
        return true
    }

    private fun allResourcesNull(resourceType: String, ids: List<String>): Boolean {
        ids.forEach {
            if (doesResourceExist(resourceType, it)) {
                return false
            }
        }
        return true
    }

    private fun doesResourceExist(resourceType: String, id: String): Boolean {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val response = try {
                aidbox.ktorClient.get("$aidboxUrl/fhir/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${aidbox.accessToken()}")
                    }
                }
            } catch (e: ClientRequestException) {
                e.response
            }

            if (response.status.isSuccess()) {
                true
            } else if (response.status == HttpStatusCode.NotFound) {
                false
            } else {
                throw IllegalStateException("Error while purging test data: ${response.bodyAsText()}")
            }
        }
    }
}
