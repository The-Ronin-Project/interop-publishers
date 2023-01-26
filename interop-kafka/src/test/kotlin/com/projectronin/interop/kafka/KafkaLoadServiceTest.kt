package com.projectronin.interop.kafka

import com.projectronin.event.interop.resource.load.v1.InteropResourceLoadV1
import com.projectronin.event.interop.resource.publish.v1.InteropResourcePublishV1
import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.Failure
import com.projectronin.interop.kafka.model.KafkaAction
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.model.LoadTopic
import com.projectronin.interop.kafka.model.PushResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class KafkaLoadServiceTest {

    private val patientTopic = mockk<LoadTopic> {
        every { resourceType } returns "Patient"
        every { systemName } returns "interop-platform"
    }

    private val appointmentTopic = mockk<LoadTopic> {
        every { resourceType } returns "Appointment"
        every { systemName } returns "interop-platform"
    }

    private val kafkaClient = mockk<KafkaClient>()
    private val tenantId = "test"
    private val service = KafkaLoadService(kafkaClient, listOf(patientTopic, appointmentTopic))

    @Test
    fun `publishing single resource is successful`() {
        val loadData = InteropResourceLoadV1(
            tenantId = tenantId,
            resourceFHIRId = "1234",
            resourceType = "PATIENT",
            dataTrigger = InteropResourceLoadV1.DataTrigger.nightly
        )
        val patientEvent = KafkaEvent("interop-platform", "resource", KafkaAction.LOAD, "1234", data = loadData)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                listOf(patientEvent)
            )
        } returns PushResponse(successful = listOf(patientEvent))

        assertDoesNotThrow {
            service.pushLoadEvent(
                tenantId,
                DataTrigger.NIGHTLY,
                listOf("1234"),
                ResourceType.PATIENT
            )
        }
    }

    @Test
    fun `publishing single resource failure`() {
        val loadData = InteropResourceLoadV1(
            tenantId = tenantId,
            resourceFHIRId = "1234",
            resourceType = "PATIENT",
            dataTrigger = InteropResourceLoadV1.DataTrigger.nightly
        )
        val patientEvent = KafkaEvent("interop-platform", "resource", KafkaAction.LOAD, "1234", data = loadData)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                listOf(patientEvent)
            )
        } returns PushResponse(failures = listOf(Failure((patientEvent), error = Exception())))

        assertEquals(
            service.pushLoadEvent(
                tenantId,
                DataTrigger.NIGHTLY,
                listOf("1234"),
                ResourceType.PATIENT
            ).failures.size,
            1
        )
    }

    @Test
    fun `publishing single resource exception`() {
        val loadData = InteropResourceLoadV1(
            tenantId = tenantId,
            resourceFHIRId = "1234",
            resourceType = "PATIENT",
            dataTrigger = InteropResourceLoadV1.DataTrigger.nightly
        )
        val patientEvent = KafkaEvent("interop-platform", "resource", KafkaAction.LOAD, "1234", data = loadData)
        every {
            kafkaClient.publishEvents(
                patientTopic,
                listOf(patientEvent)
            )
        } throws Exception("error")

        assertEquals(
            service.pushLoadEvent(
                tenantId,
                DataTrigger.NIGHTLY,
                listOf("1234"),
                ResourceType.PATIENT
            ).failures.size,
            1
        )
    }

    @Test
    fun `publishing single resource no topic`() {
        assertEquals(
            service.pushLoadEvent(
                tenantId,
                DataTrigger.NIGHTLY,
                listOf("1234"),
                ResourceType.PRACTITIONER
            ).failures.size,
            1
        )
    }

    @Test
    fun `retrieve events works`() {
        val loadData = InteropResourceLoadV1(
            tenantId = tenantId,
            resourceFHIRId = "1234",
            resourceType = "PATIENT",
            dataTrigger = InteropResourceLoadV1.DataTrigger.nightly
        )
        every { kafkaClient.retrieveEvents(any(), any()) } returns listOf(mockk { every { data } returns loadData })
        val ret = service.retrieveLoadEvents(ResourceType.PATIENT)
        assertEquals(loadData, ret.first())
    }

    @Test
    fun `retrieve events empty`() {
        val loadData = InteropResourceLoadV1(
            tenantId = tenantId,
            resourceFHIRId = "1234",
            resourceType = "PATIENT",
            dataTrigger = InteropResourceLoadV1.DataTrigger.nightly
        )
        every { kafkaClient.retrieveEvents(any(), any()) } returns listOf(mockk { every { data } returns loadData })
        val ret = service.retrieveLoadEvents(ResourceType.PRACTITIONER)
        assertEquals(emptyList<InteropResourcePublishV1>(), ret)
    }
}
