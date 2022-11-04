package com.projectronin.interop.kafka

import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Participant
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.kafka.event.AppointmentEvent
import com.projectronin.interop.kafka.event.PatientEvent
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.PublishTopic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KafkaPublishServiceIT : BaseKafkaIT() {
    private val patientTopic = PublishTopic(
        tenantSpecific = true,
        systemName = "interop",
        eventType = "patient",
        version = "v1",
        dataSchema = "http://localhost/event/interop.patient",
        resourceType = "Patient",
        converter = { tenant, resource ->
            PatientEvent(
                tenantId = tenant,
                patientJson = objectMapper.writeValueAsString(resource)
            )
        }
    )
    private val appointmentTopic = PublishTopic(
        tenantSpecific = true,
        systemName = "interop",
        eventType = "appointment",
        version = "v1",
        dataSchema = "http://localhost/event/interop.appointment",
        resourceType = "Appointment",
        converter = { tenant, resource ->
            AppointmentEvent(
                tenantId = tenant,
                appointmentJson = objectMapper.writeValueAsString(resource)
            )
        }
    )

    private val kafkaClient = KafkaClient(kafkaConfig)
    private val publishService = KafkaPublishService(kafkaClient, listOf(patientTopic, appointmentTopic))

    @Test
    fun `can publish a single resource`() {
        val patient = Patient(
            id = Id("12345"),
            name = listOf(
                HumanName(
                    family = "Public",
                    given = listOf("John", "Q")
                )
            )
        )

        val response = publishService.publishResources(tenantId, DataTrigger.AD_HOC, listOf(patient))
        assertEquals(1, response.successful.size)
        assertEquals(patient, response.successful[0])

        assertEquals(0, response.failures.size)

        val publishedEvents =
            pollEvents(patientTopic, DataTrigger.AD_HOC, mapOf("ronin.interop.patient.publish" to PatientEvent::class))
        assertEquals(1, publishedEvents.size)

        assertEquals(
            PatientEvent(
                tenantId = tenantId,
                patientJson = objectMapper.writeValueAsString(patient)
            ),
            publishedEvents[0].data
        )
    }

    @Test
    fun `can publish multiple resources of same type`() {
        val patient1 = Patient(
            id = Id("12345"),
            name = listOf(
                HumanName(
                    family = "Public",
                    given = listOf("John", "Q")
                )
            )
        )
        val patient2 = Patient(
            id = Id("67890"),
            name = listOf(
                HumanName(
                    family = "Doe",
                    given = listOf("Jane")
                )
            )
        )

        val response = publishService.publishResources(tenantId, DataTrigger.AD_HOC, listOf(patient1, patient2))
        assertEquals(2, response.successful.size)
        assertEquals(patient1, response.successful[0])
        assertEquals(patient2, response.successful[1])

        assertEquals(0, response.failures.size)

        val publishedEvents =
            pollEvents(patientTopic, DataTrigger.AD_HOC, mapOf("ronin.interop.patient.publish" to PatientEvent::class))
        assertEquals(2, publishedEvents.size)

        assertEquals(
            PatientEvent(
                tenantId = tenantId,
                patientJson = objectMapper.writeValueAsString(patient1)
            ),
            publishedEvents[0].data
        )
        assertEquals(
            PatientEvent(
                tenantId = tenantId,
                patientJson = objectMapper.writeValueAsString(patient2)
            ),
            publishedEvents[1].data
        )
    }

    @Test
    fun `can publish multiple resources of differing type`() {
        val patient1 = Patient(
            id = Id("12345"),
            name = listOf(
                HumanName(
                    family = "Public",
                    given = listOf("John", "Q")
                )
            )
        )
        val appointment1 = Appointment(
            id = Id("67890"),
            participant = listOf(
                Participant(
                    actor = Reference(
                        reference = "Patient/12345"
                    ),
                    status = Code("accepted")
                )
            ),
            status = Code("fulfilled")
        )

        val response = publishService.publishResources(tenantId, DataTrigger.AD_HOC, listOf(patient1, appointment1))
        assertEquals(2, response.successful.size)
        assertTrue(response.successful.contains(patient1))
        assertTrue(response.successful.contains(appointment1))

        assertEquals(0, response.failures.size)

        val publishedPatientEvents =
            pollEvents(
                patientTopic, DataTrigger.AD_HOC,
                mapOf(
                    "ronin.interop.patient.publish" to PatientEvent::class
                )
            )
        assertEquals(1, publishedPatientEvents.size)
        assertEquals(
            PatientEvent(
                tenantId = tenantId,
                patientJson = objectMapper.writeValueAsString(patient1)
            ),
            publishedPatientEvents[0].data
        )

        val publishedAppointmentEvents =
            pollEvents(
                appointmentTopic, DataTrigger.AD_HOC,
                mapOf(
                    "ronin.interop.appointment.publish" to AppointmentEvent::class
                )
            )
        assertEquals(1, publishedAppointmentEvents.size)
        assertEquals(
            AppointmentEvent(
                tenantId = tenantId,
                appointmentJson = objectMapper.writeValueAsString(appointment1)
            ),
            publishedAppointmentEvents[0].data
        )
    }
}
