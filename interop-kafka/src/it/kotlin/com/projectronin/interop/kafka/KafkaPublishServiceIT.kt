package com.projectronin.interop.kafka

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.common.resource.ResourceType
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.Participant
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.spring.PublishSpringConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KafkaPublishServiceIT : BaseKafkaIT() {
    private val topics = PublishSpringConfig().publishTopics()

    private val kafkaClient = KafkaClient(kafkaConfig)
    private val publishService = KafkaPublishService(kafkaClient, topics)

    @Test
    fun `can publish a single resource`() {
        val patient = Patient(
            id = Id("12345"),
            name = listOf(
                HumanName(
                    family = "Public".asFHIR(),
                    given = listOf("John", "Q").asFHIR()
                )
            ),
            gender = AdministrativeGender.MALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val response = publishService.publishResources(tenantId, DataTrigger.AD_HOC, listOf(patient))
        assertEquals(1, response.successful.size)
        assertEquals(patient, response.successful[0])

        assertEquals(0, response.failures.size)

        val publishedEvents = publishService.retrievePublishEvents(ResourceType.PATIENT, DataTrigger.AD_HOC)
        assertEquals(1, publishedEvents.size)
        assertEquals(JacksonUtil.writeJsonValue(patient), publishedEvents.first().resourceJson)
    }

    @Test
    fun `can publish multiple resources of same type`() {
        val patient1 = Patient(
            id = Id("12345"),
            name = listOf(
                HumanName(
                    family = "Public".asFHIR(),
                    given = listOf("John", "Q").asFHIR()
                )
            ),
            gender = AdministrativeGender.MALE.asCode(),
            birthDate = Date("1975-07-05")
        )
        val patient2 = Patient(
            id = Id("67890"),
            name = listOf(
                HumanName(
                    family = "Doe".asFHIR(),
                    given = listOf("Jane").asFHIR()
                )
            ),
            gender = AdministrativeGender.FEMALE.asCode(),
            birthDate = Date("1975-07-05")
        )

        val response = publishService.publishResources(tenantId, DataTrigger.AD_HOC, listOf(patient1, patient2))
        assertEquals(2, response.successful.size)
        assertEquals(patient1, response.successful[0])
        assertEquals(patient2, response.successful[1])

        assertEquals(0, response.failures.size)
    }

    @Test
    fun `can publish multiple resources of differing type`() {
        val patient1 = Patient(
            id = Id("12345"),
            name = listOf(
                HumanName(
                    family = "Public".asFHIR(),
                    given = listOf("John", "Q").asFHIR()
                )
            ),
            gender = AdministrativeGender.MALE.asCode(),
            birthDate = Date("1975-07-05")
        )
        val appointment1 = Appointment(
            id = Id("67890"),
            participant = listOf(
                Participant(
                    actor = Reference(
                        reference = "Patient/12345".asFHIR()
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
    }
}
