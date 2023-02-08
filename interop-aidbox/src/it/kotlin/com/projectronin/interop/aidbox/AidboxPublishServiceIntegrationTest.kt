package com.projectronin.interop.aidbox

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.projectronin.interop.aidbox.spring.AidboxSpringConfig
import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.BaseAidboxTest
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.AvailableTime
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.LocationHoursOfOperation
import com.projectronin.interop.fhir.r4.resource.LocationPosition
import com.projectronin.interop.fhir.r4.resource.NotAvailable
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.PatientCommunication
import com.projectronin.interop.fhir.r4.resource.PatientContact
import com.projectronin.interop.fhir.r4.resource.PatientLink
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
import com.projectronin.interop.fhir.util.asCode
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
@ContextConfiguration(classes = [AidboxSpringConfig::class])
class AidboxPublishServiceIntegrationTest : BaseAidboxTest() {
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
    private lateinit var aidboxPublishService: AidboxPublishService

    private val tenantIdentifier = Identifier(
        type = CodeableConcept(
            coding = listOf(
                Coding(
                    system = CodeSystem.RONIN_TENANT.uri,
                    code = Code("TID"),
                    display = "Ronin-specified Tenant Identifier".asFHIR()
                )
            )
        ),
        system = CodeSystem.RONIN_TENANT.uri,
        value = "mdaoc".asFHIR()
    )

    @Test
    fun `can publish a new resource`() {
        // Verify that the resource does not exist.
        val initialResource = getResource<Practitioner>("Practitioner", "mdaoc-new-resource")
        assertNull(initialResource)

        val practitioner = Practitioner(
            id = Id("mdaoc-new-resource"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Smith".asFHIR(), given = listOf("Josh").asFHIR()))
        )
        val published = aidboxPublishService.publish(listOf(practitioner))
        assertTrue(published)

        val resource = getResource<Practitioner>("Practitioner", "mdaoc-new-resource")
        assertEquals(practitioner, resource)
    }

    @Test
    @AidboxData("/aidbox/publish/PractitionerToUpdate.yaml")
    fun `can publish an updated resource`() {
        val practitioner = Practitioner(
            id = Id("mdaoc-existing-resource"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Doctor".asFHIR(), given = listOf("Bob").asFHIR()))
        )

        // Verify that the resource does exist.
        val initialResource = getResource<Practitioner>("Practitioner", "mdaoc-existing-resource")
        assertNotNull(initialResource)
        // And that it isn't what we are about to make it.
        assertNotEquals(practitioner, initialResource)

        val published = aidboxPublishService.publish(listOf(practitioner))
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
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR()))
        )
        val practitioner2 = Practitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Llyr".asFHIR(), given = listOf("Regan", "Anne").asFHIR()))
        )
        val testPractitioners = listOf(practitioner1, practitioner2)
        assertTrue(allResourcesNull("Practitioner", listOf("${idPrefix}cmjones", "${idPrefix}rallyr")))

        // Test
        val published = aidboxPublishService.publish(testPractitioners)
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
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "My Office".asFHIR(),
            alias = listOf("Guest Room").asFHIR(),
            description = "Sun Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
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
                        DayOfWeek.SATURDAY.asCode(),
                        DayOfWeek.SUNDAY.asCode()
                    ),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "Call for details".asFHIR()
        )
        val location2 = Location(
            id = Id("${idPrefix}12346"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "Back Study".asFHIR(),
            alias = listOf("Studio").asFHIR(),
            description = "Game Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "123-456-7890".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(DayOfWeek.TUESDAY.asCode()),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "By appointment".asFHIR()
        )
        val testLocations = listOf(location1, location2)
        assertTrue(allResourcesNull("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))

        // Test
        val published = aidboxPublishService.publish(testLocations)
        assertTrue(published)
        assertTrue(allResourcesExist("Location", listOf("${idPrefix}12345", "${idPrefix}12346")))

        // After
        deleteAllResources("Location", listOf("${idPrefix}12345", "${idPrefix}12346"))
    }

    @Test
    fun `can publish multiple resources of different resourceTypes (both RoninResource)`() {
        val practitioner = Practitioner(
            id = Id("mdaoc-practitioner"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Doctor".asFHIR(), given = listOf("Bob").asFHIR()))
        )
        val patient = Patient(
            id = Id("mdaoc-patient"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Doe".asFHIR(), given = listOf("John").asFHIR())),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.EMAIL.asCode(),
                    value = "john.doe@projectronin.com".asFHIR(),
                    use = ContactPointUse.WORK.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode(),
            birthDate = Date("1976-02-16"),
            address = listOf(Address(text = "Address".asFHIR())),
            maritalStatus = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus"),
                        code = Code("U")
                    )
                ),
                text = "Unmarried".asFHIR()
            )
        )

        val published = aidboxPublishService.publish(listOf(practitioner, patient))
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
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR()))
        )
        val practitioner2 = Practitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Llyr".asFHIR(), given = listOf("Regan", "Anne").asFHIR()))
        )
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "My Office".asFHIR(),
            alias = listOf("Guest Room").asFHIR(),
            description = "Sun Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
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
                        DayOfWeek.SATURDAY.asCode(),
                        DayOfWeek.SUNDAY.asCode()
                    ),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "Call for details".asFHIR()
        )
        val location2 = Location(
            id = Id("${idPrefix}12346"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "Back Study".asFHIR(),
            alias = listOf("Studio").asFHIR(),
            description = "Game Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "123-456-7890".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(DayOfWeek.TUESDAY.asCode()),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "By appointment".asFHIR()
        )
        val practitionerRole1 = PractitionerRole(
            id = Id("${idPrefix}12347"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR()),
            location = listOf(Reference(reference = "Location/${idPrefix}12345".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.FALSE)),
            notAvailable = listOf(NotAvailable(description = "Not available now".asFHIR())),
            availabilityExceptions = "exceptions".asFHIR()
        )
        val practitionerRole2 = PractitionerRole(
            id = Id("${idPrefix}12348"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}rallyr".asFHIR()),
            location = listOf(Reference(reference = "Location/${idPrefix}12346".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.TRUE)),
            notAvailable = listOf(NotAvailable(description = "Available now".asFHIR())),
            availabilityExceptions = "No exceptions".asFHIR()
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
        val published = aidboxPublishService.publish(fullRoles)
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
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR()))
        )
        val practitioner2 = Practitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Llyr".asFHIR(), given = listOf("Regan", "Anne").asFHIR()))
        )
        val patient = Patient(
            id = Id("${idPrefix}12345"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            name = listOf(HumanName(family = "Doe".asFHIR())),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE.asCode(),
                    value = "8675309".asFHIR(),
                    use = ContactPointUse.MOBILE.asCode()
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(type = DynamicValueType.BOOLEAN, value = false),
            address = listOf(Address(country = "USA".asFHIR())),
            maritalStatus = CodeableConcept(text = "M".asFHIR()),
            multipleBirth = DynamicValue(type = DynamicValueType.INTEGER, value = 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(PatientContact(name = HumanName(text = "Jane Doe".asFHIR()))),
            communication = listOf(PatientCommunication(language = CodeableConcept(text = "English".asFHIR()))),
            generalPractitioner = listOf(Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR())),
            managingOrganization = Reference(display = "organization".asFHIR()),
            link = listOf(
                PatientLink(
                    other = Reference(display = "Patient".asFHIR()),
                    type = LinkType.REPLACES.asCode()
                )
            )
        )
        val appointment = Appointment(
            id = Id("${idPrefix}12345"),
            identifier = listOf(tenantIdentifier),
            status = AppointmentStatus.CANCELLED.asCode(),
            appointmentType = CodeableConcept(text = "appointment type".asFHIR()),
            cancelationReason = CodeableConcept(text = "cancel reason".asFHIR()),
            serviceCategory = listOf(CodeableConcept(text = "service category".asFHIR())),
            serviceType = listOf(CodeableConcept(text = "service type".asFHIR())),
            specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
            reasonCode = listOf(CodeableConcept(text = "reason code".asFHIR())),
            reasonReference = listOf(Reference(display = "reason reference".asFHIR())),
            priority = 1.asFHIR(),
            description = "appointment test".asFHIR(),
            start = Instant(value = "2017-01-01T00:00:00Z"),
            end = Instant(value = "2017-01-01T01:00:00Z"),
            minutesDuration = 15.asFHIR(),
            slot = listOf(Reference(display = "slot".asFHIR())),
            created = DateTime(value = "2021-11-16"),
            comment = "comment".asFHIR(),
            patientInstruction = "patient instruction".asFHIR(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Patient/${idPrefix}12345".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}rallyr".asFHIR()),
                    status = ParticipationStatus.DECLINED.asCode()
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
        val published = aidboxPublishService.publish(fullAppointment)
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
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR()))
        )
        val practitioner2 = Practitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Llyr".asFHIR(), given = listOf("Regan", "Anne").asFHIR()))
        )
        val practitioner3 = Practitioner(
            id = Id("${idPrefix}gwalsh"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Walsh".asFHIR(), given = listOf("Goneril").asFHIR()))
        )
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "My Office".asFHIR(),
            alias = listOf("Guest Room").asFHIR(),
            description = "Sun Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
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
                        DayOfWeek.SATURDAY.asCode(),
                        DayOfWeek.SUNDAY.asCode()
                    ),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "Call for details".asFHIR()
        )
        val location2 = Location(
            id = Id("${idPrefix}12346"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "Back Study".asFHIR(),
            alias = listOf("Studio").asFHIR(),
            description = "Game Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "123-456-7890".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(DayOfWeek.TUESDAY.asCode()),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "By appointment".asFHIR()
        )
        val practitionerRole1 = PractitionerRole(
            id = Id("${idPrefix}12347"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR()),
            location = listOf(Reference(reference = "Location/${idPrefix}12345".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.FALSE)),
            notAvailable = listOf(NotAvailable(description = "Not available now".asFHIR())),
            availabilityExceptions = "exceptions".asFHIR()
        )
        val practitionerRole2 = PractitionerRole(
            id = Id("${idPrefix}12348"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}rallyr".asFHIR()),
            location = listOf(Reference(reference = "Location/${idPrefix}12346".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.TRUE)),
            notAvailable = listOf(NotAvailable(description = "Available now".asFHIR())),
            availabilityExceptions = "No exceptions".asFHIR()
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
        val published = aidboxPublishService.publish(unrelatedResourceInList)
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
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR()))
        )
        val practitioner2 = Practitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Llyr".asFHIR(), given = listOf("Regan", "Anne").asFHIR()))
        )
        val practitioner3 = Practitioner(
            id = Id("${idPrefix}gwalsh"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Walsh".asFHIR(), given = listOf("Goneril").asFHIR()))
        )
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "My Office".asFHIR(),
            alias = listOf("Guest Room").asFHIR(),
            description = "Sun Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
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
                        DayOfWeek.SATURDAY.asCode(),
                        DayOfWeek.SUNDAY.asCode()
                    ),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "Call for details".asFHIR()
        )
        val location2 = Location(
            id = Id("${idPrefix}12346"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "Back Study".asFHIR(),
            alias = listOf("Studio").asFHIR(),
            description = "Game Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "123-456-7890".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
                coding = listOf(
                    Coding(
                        code = Code("ro"),
                        system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
                    )
                )
            ),
            hoursOfOperation = listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(DayOfWeek.TUESDAY.asCode()),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "By appointment".asFHIR()
        )
        val patient = Patient(
            id = Id("${idPrefix}12345"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            name = listOf(HumanName(family = "Doe".asFHIR())),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE.asCode(),
                    value = "8675309".asFHIR(),
                    use = ContactPointUse.MOBILE.asCode()
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(type = DynamicValueType.BOOLEAN, value = false),
            address = listOf(Address(country = "USA".asFHIR())),
            maritalStatus = CodeableConcept(text = "M".asFHIR()),
            multipleBirth = DynamicValue(type = DynamicValueType.INTEGER, value = 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(PatientContact(name = HumanName(text = "Jane Doe".asFHIR()))),
            communication = listOf(PatientCommunication(language = CodeableConcept(text = "English".asFHIR()))),
            generalPractitioner = listOf(Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR())),
            managingOrganization = Reference(display = "organization".asFHIR()),
            link = listOf(
                PatientLink(
                    other = Reference(display = "Patient".asFHIR()),
                    type = LinkType.REPLACES.asCode()
                )
            )
        )
        val appointment = Appointment(
            id = Id("${idPrefix}12345"),
            identifier = listOf(tenantIdentifier),
            status = AppointmentStatus.CANCELLED.asCode(),
            appointmentType = CodeableConcept(text = "appointment type".asFHIR()),
            cancelationReason = CodeableConcept(text = "cancel reason".asFHIR()),
            serviceCategory = listOf(CodeableConcept(text = "service category".asFHIR())),
            serviceType = listOf(CodeableConcept(text = "service type".asFHIR())),
            specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
            reasonCode = listOf(CodeableConcept(text = "reason code".asFHIR())),
            reasonReference = listOf(Reference(display = "reason reference".asFHIR())),
            priority = 1.asFHIR(),
            description = "appointment test".asFHIR(),
            start = Instant(value = "2017-01-01T00:00:00Z"),
            end = Instant(value = "2017-01-01T01:00:00Z"),
            minutesDuration = 15.asFHIR(),
            slot = listOf(Reference(display = "slot".asFHIR())),
            created = DateTime(value = "2021-11-16"),
            comment = "comment".asFHIR(),
            patientInstruction = "patient instruction".asFHIR(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Patient/${idPrefix}12345".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}rallyr".asFHIR()),
                    status = ParticipationStatus.DECLINED.asCode()
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
        val published = aidboxPublishService.publish(unrelatedResourceInList)
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
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR()))
        )
        val practitioner2 = Practitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Llyr".asFHIR(), given = listOf("Regan", "Anne").asFHIR()))
        )
        val location1 = Location(
            id = Id("${idPrefix}12345"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
            identifier = listOf(tenantIdentifier),
            mode = LocationMode.INSTANCE.asCode(),
            status = LocationStatus.ACTIVE.asCode(),
            name = "My Office".asFHIR(),
            alias = listOf("Guest Room").asFHIR(),
            description = "Sun Room".asFHIR(),
            type = listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding = listOf(
                        Coding(
                            code = Code("DX"),
                            system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                        )
                    )
                )
            ),
            telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE.asCode(), value = "8675309".asFHIR())),
            address = Address(country = "USA".asFHIR()),
            physicalType = CodeableConcept(
                text = "Room".asFHIR(),
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
                        DayOfWeek.SATURDAY.asCode(),
                        DayOfWeek.SUNDAY.asCode()
                    ),
                    allDay = FHIRBoolean.TRUE
                )
            ),
            availabilityExceptions = "Call for details".asFHIR()
        )
        val practitionerRole1 = PractitionerRole(
            id = Id("${idPrefix}12347"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR()),
            location = listOf(Reference(reference = "Location/${idPrefix}12345".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.FALSE)),
            notAvailable = listOf(NotAvailable(description = "Not available now".asFHIR())),
            availabilityExceptions = "exceptions".asFHIR()
        )
        val practitionerRole2 = PractitionerRole(
            id = Id("${idPrefix}12348"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            period = Period(end = DateTime("2022")),
            practitioner = Reference(reference = "Practitioner/${idPrefix}rallyr".asFHIR()),
            location = listOf(Reference(reference = "Location/${idPrefix}12346".asFHIR())),
            availableTime = listOf(AvailableTime(allDay = FHIRBoolean.TRUE)),
            notAvailable = listOf(NotAvailable(description = "Available now".asFHIR())),
            availabilityExceptions = "No exceptions".asFHIR()
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
        val published = aidboxPublishService.publish(practitionerRoles)
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
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR()))
        )
        val practitioner2 = Practitioner(
            id = Id("${idPrefix}rallyr"),
            identifier = listOf(tenantIdentifier),
            name = listOf(HumanName(family = "Llyr".asFHIR(), given = listOf("Regan", "Anne").asFHIR()))
        )
        val patient = Patient(
            id = Id("${idPrefix}12345"),
            identifier = listOf(tenantIdentifier),
            active = FHIRBoolean.TRUE,
            name = listOf(HumanName(family = "Doe".asFHIR())),
            telecom = listOf(
                ContactPoint(
                    system = ContactPointSystem.PHONE.asCode(),
                    value = "8675309".asFHIR(),
                    use = ContactPointUse.MOBILE.asCode()
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05"),
            deceased = DynamicValue(type = DynamicValueType.BOOLEAN, value = false),
            address = listOf(Address(country = "USA".asFHIR())),
            maritalStatus = CodeableConcept(text = "M".asFHIR()),
            multipleBirth = DynamicValue(type = DynamicValueType.INTEGER, value = 2),
            photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            contact = listOf(PatientContact(name = HumanName(text = "Jane Doe".asFHIR()))),
            communication = listOf(PatientCommunication(language = CodeableConcept(text = "English".asFHIR()))),
            generalPractitioner = listOf(Reference(reference = "Practitioner/${idPrefix}cmjones".asFHIR())),
            managingOrganization = Reference(display = "organization".asFHIR()),
            link = listOf(
                PatientLink(
                    other = Reference(display = "Patient".asFHIR()),
                    type = LinkType.REPLACES.asCode()
                )
            )
        )
        val appointment = Appointment(
            id = Id("${idPrefix}12345"),
            identifier = listOf(
                tenantIdentifier
            ),
            status = AppointmentStatus.CANCELLED.asCode(),
            appointmentType = CodeableConcept(text = "appointment type".asFHIR()),
            cancelationReason = CodeableConcept(text = "cancel reason".asFHIR()),
            serviceCategory = listOf(CodeableConcept(text = "service category".asFHIR())),
            serviceType = listOf(CodeableConcept(text = "service type".asFHIR())),
            specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
            reasonCode = listOf(CodeableConcept(text = "reason code".asFHIR())),
            reasonReference = listOf(Reference(display = "reason reference".asFHIR())),
            priority = 1.asFHIR(),
            description = "appointment test".asFHIR(),
            start = Instant(value = "2017-01-01T00:00:00Z"),
            end = Instant(value = "2017-01-01T01:00:00Z"),
            minutesDuration = 15.asFHIR(),
            slot = listOf(Reference(display = "slot".asFHIR())),
            created = DateTime(value = "2021-11-16"),
            comment = "comment".asFHIR(),
            patientInstruction = "patient instruction".asFHIR(),
            participant = listOf(
                Participant(
                    actor = Reference(reference = "Patient/${idPrefix}12345".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}12345".asFHIR()),
                    status = ParticipationStatus.ACCEPTED.asCode()
                ),
                Participant(
                    actor = Reference(reference = "Practitioner/${idPrefix}rallyr".asFHIR()),
                    status = ParticipationStatus.DECLINED.asCode()
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
        val published = aidboxPublishService.publish(resourceList)
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
        val published = aidboxPublishService.publish(collection)
        assertTrue(published)
    }

    @Test
    fun `can publish relatively large number of resources`() {
        // this number should exceed our batch size, which currently defaults to 25
        val resources = (1..100).map {
            Practitioner(
                id = Id("batchTest-$it"),
                identifier = listOf(tenantIdentifier),
                name = listOf(HumanName(family = "Jones".asFHIR(), given = listOf(FHIRString("$it"))))
            )
        }
        val resourceIds = resources.map { it.id!!.value!! }

        assertTrue(allResourcesNull("Practitioner", resourceIds))

        val published = aidboxPublishService.publish(resources)
        assertTrue(published)
        assertTrue(allResourcesExist("Practitioner", resourceIds))

        deleteAllResources("Practitioner", resourceIds)
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

    private fun deleteResource(resourceType: String, id: String, accessToken: String = aidbox.accessToken()) {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            try {
                aidbox.ktorClient.delete("$aidboxUrl/fhir/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
                    }
                }
            } catch (e: ClientRequestException) {
            } // May examine exception or response if needed, not important to existing tests
        }
    }

    private fun deleteAllResources(resourceType: String, ids: List<String>) {
        val accessToken = aidbox.accessToken()
        ids.forEach {
            deleteResource(resourceType, it, accessToken)
        }
    }

    private fun allResourcesExist(resourceType: String, ids: List<String>): Boolean {
        val accessToken = aidbox.accessToken()
        ids.forEach {
            if (!doesResourceExist(resourceType, it, accessToken)) {
                return false
            }
        }
        return true
    }

    private fun allResourcesNull(resourceType: String, ids: List<String>): Boolean {
        val accessToken = aidbox.accessToken()
        ids.forEach {
            if (doesResourceExist(resourceType, it, accessToken)) {
                return false
            }
        }
        return true
    }

    private fun doesResourceExist(
        resourceType: String,
        id: String,
        accessToken: String = aidbox.accessToken()
    ): Boolean {
        return runBlocking {
            val aidboxUrl = aidbox.baseUrl()
            val response = try {
                aidbox.ktorClient.get("$aidboxUrl/fhir/$resourceType/$id") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
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
