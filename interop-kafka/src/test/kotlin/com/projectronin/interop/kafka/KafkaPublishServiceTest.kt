package com.projectronin.interop.kafka

import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.Failure
import com.projectronin.interop.kafka.model.KafkaAction
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.model.PublishResponse
import com.projectronin.interop.kafka.model.PublishTopic
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KafkaPublishServiceTest {
    private val selfConverter = { _: String, resource: Resource<*> -> resource }

    private val patientTopic = mockk<PublishTopic> {
        every { resourceType } returns "Patient"
        every { systemName } returns "interop"
        every { converter } returns selfConverter
    }

    private val appointmentTopic = mockk<PublishTopic> {
        every { resourceType } returns "Appointment"
        every { systemName } returns "interop"
        every { converter } returns selfConverter
    }

    private val kafkaClient = mockk<KafkaClient>()
    private val tenantId = "test"
    private val service = KafkaPublishService(kafkaClient, listOf(patientTopic, appointmentTopic))

    @Test
    fun `publishing single resource is successful`() {
        val patient = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("1234")
        }

        val patientEvent = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent)
            )
        } returns PublishResponse(successful = listOf(patientEvent))

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient))
        assertEquals(1, response.successful.size)
        assertEquals(patient, response.successful[0])

        assertEquals(0, response.failures.size)
    }

    @Test
    fun `publishing single resource has failure`() {
        val patient = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("1234")
        }

        val patientEvent = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent)
            )
        } returns PublishResponse(failures = listOf(Failure(patientEvent, IllegalStateException("exception"))))

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient))
        assertEquals(0, response.successful.size)

        assertEquals(1, response.failures.size)
        assertEquals(patient, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals("exception", response.failures[0].error.message)
    }

    @Test
    fun `publishing single resource throws exception`() {
        val patient = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("1234")
        }

        val patientEvent = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent)
            )
        } throws IllegalStateException("exception")

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient))
        assertEquals(0, response.successful.size)

        assertEquals(1, response.failures.size)
        assertEquals(patient, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals("exception", response.failures[0].error.message)
    }

    @Test
    fun `publishing multiple resources of same type throws exception`() {
        val patient1 = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("1234")
        }
        val patient2 = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("5678")
        }

        val patientEvent1 = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient1)
        val patientEvent2 = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "5678", patient2)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent1, patientEvent2)
            )
        } throws IllegalStateException("exception")

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient1, patient2))
        assertEquals(0, response.successful.size)

        assertEquals(2, response.failures.size)
        assertEquals(patient1, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals("exception", response.failures[0].error.message)

        assertEquals(patient2, response.failures[1].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[1].error)
        assertEquals("exception", response.failures[1].error.message)
    }

    @Test
    fun `some resources do not succeed when multiple supplied`() {
        val patient = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("1234")
        }
        val appointment = mockk<Appointment> {
            every { resourceType } returns "Appointment"
            every { id } returns Id("5678")
        }

        val patientEvent = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent)
            )
        } returns PublishResponse(successful = listOf(patientEvent))

        val appointmentEvent = KafkaEvent("interop", "appointment", KafkaAction.PUBLISH, "5678", appointment)
        every {
            kafkaClient.publishEvents(
                appointmentTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(appointmentEvent)
            )
        } throws IllegalStateException("exception")

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient, appointment))
        assertEquals(1, response.successful.size)
        assertEquals(patient, response.successful[0])

        assertEquals(1, response.failures.size)
        assertEquals(appointment, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals("exception", response.failures[0].error.message)
    }

    @Test
    fun `all resources publish when multiple supplied`() {
        val patient = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("1234")
        }
        val appointment = mockk<Appointment> {
            every { resourceType } returns "Appointment"
            every { id } returns Id("5678")
        }

        val patientEvent = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent)
            )
        } returns PublishResponse(successful = listOf(patientEvent))

        val appointmentEvent = KafkaEvent("interop", "appointment", KafkaAction.PUBLISH, "5678", appointment)
        every {
            kafkaClient.publishEvents(
                appointmentTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(appointmentEvent)
            )
        } returns PublishResponse(successful = listOf(appointmentEvent))

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient, appointment))
        assertEquals(2, response.successful.size)
        assertTrue(response.successful.contains(patient))
        assertTrue(response.successful.contains(appointment))

        assertEquals(0, response.failures.size)
    }

    @Test
    fun `supports multiple resources of the same type`() {
        val patient1 = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("1234")
        }
        val patient2 = mockk<Patient> {
            every { resourceType } returns "Patient"
            every { id } returns Id("3456")
        }
        val appointment1 = mockk<Appointment> {
            every { resourceType } returns "Appointment"
            every { id } returns Id("5678")
        }
        val appointment2 = mockk<Appointment> {
            every { resourceType } returns "Appointment"
            every { id } returns Id("7890")
        }

        val patientEvent1 = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient1)
        val patientEvent2 = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "3456", patient2)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent1, patientEvent2)
            )
        } returns PublishResponse(successful = listOf(patientEvent1, patientEvent2))

        val appointmentEvent1 = KafkaEvent("interop", "appointment", KafkaAction.PUBLISH, "5678", appointment1)
        val appointmentEvent2 = KafkaEvent("interop", "appointment", KafkaAction.PUBLISH, "7890", appointment2)
        every {
            kafkaClient.publishEvents(
                appointmentTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(appointmentEvent1, appointmentEvent2)
            )
        } returns PublishResponse(successful = listOf(appointmentEvent1, appointmentEvent2))

        val response = service.publishResources(
            tenantId,
            DataTrigger.NIGHTLY,
            listOf(patient1, patient2, appointment1, appointment2)
        )
        assertEquals(4, response.successful.size)
        assertTrue(response.successful.contains(patient1))
        assertTrue(response.successful.contains(patient2))
        assertTrue(response.successful.contains(appointment1))
        assertTrue(response.successful.contains(appointment2))

        assertEquals(0, response.failures.size)
    }

    @Test
    fun `resource types are case-insensitive`() {
        val patient = mockk<Patient> {
            every { resourceType } returns "PaTiEnT"
            every { id } returns Id("1234")
        }

        val patientEvent = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent)
            )
        } returns PublishResponse(successful = listOf(patientEvent))

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient))
        assertEquals(1, response.successful.size)
        assertEquals(patient, response.successful[0])

        assertEquals(0, response.failures.size)
    }

    @Test
    fun `no topic associated to resource type`() {
        val practitioner = mockk<Practitioner> {
            every { resourceType } returns "Practitioner"
            every { id } returns Id("1234")
        }

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(practitioner))
        assertEquals(0, response.successful.size)

        assertEquals(1, response.failures.size)
        assertEquals(practitioner, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals(
            "Zero or multiple PublishTopics associated to resource type practitioner",
            response.failures[0].error.message
        )

        verify(exactly = 0) { kafkaClient.publishEvents<Any>(any(), any(), any(), any()) }
    }

    @Test
    fun `no topic associated to resource type with multiple resources`() {
        val practitioner1 = mockk<Practitioner> {
            every { resourceType } returns "Practitioner"
            every { id } returns Id("1234")
        }
        val practitioner2 = mockk<Practitioner> {
            every { resourceType } returns "Practitioner"
            every { id } returns Id("5678")
        }

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(practitioner1, practitioner2))
        assertEquals(0, response.successful.size)

        assertEquals(2, response.failures.size)
        assertEquals(practitioner1, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals(
            "Zero or multiple PublishTopics associated to resource type practitioner",
            response.failures[0].error.message
        )

        assertEquals(practitioner2, response.failures[1].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[1].error)
        assertEquals(
            "Zero or multiple PublishTopics associated to resource type practitioner",
            response.failures[1].error.message
        )

        verify(exactly = 0) { kafkaClient.publishEvents<Any>(any(), any(), any(), any()) }
    }

    @Test
    fun `other resources published when no topic associated to one of supplied types`() {
        val patient = mockk<Patient> {
            every { resourceType } returns "PaTiEnT"
            every { id } returns Id("1234")
        }
        val practitioner = mockk<Practitioner> {
            every { resourceType } returns "Practitioner"
            every { id } returns Id("1234")
        }

        val patientEvent = KafkaEvent("interop", "patient", KafkaAction.PUBLISH, "1234", patient)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                tenantId,
                DataTrigger.NIGHTLY,
                listOf(patientEvent)
            )
        } returns PublishResponse(successful = listOf(patientEvent))

        val response = service.publishResources(tenantId, DataTrigger.NIGHTLY, listOf(patient, practitioner))
        assertEquals(1, response.successful.size)
        assertEquals(patient, response.successful[0])

        assertEquals(1, response.failures.size)
        assertEquals(practitioner, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals(
            "Zero or multiple PublishTopics associated to resource type practitioner",
            response.failures[0].error.message
        )

        verify(exactly = 1) { kafkaClient.publishEvents<Any>(any(), any(), any(), any()) }
    }
}
