package com.projectronin.interop.kafka.spring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TopicConfigTest {

    @Test
    fun `get topics`() {
        val supportedResources = listOf(
            "Patient",
            "Binary",
            "Practitioner",
            "Appointment",
            "CarePlan",
            "CareTeam",
            "Communication",
            "Condition",
            "DocumentReference",
            "Encounter",
            "Location",
            "Medication",
            "MedicationRequest",
            "MedicationStatement",
            "Observation",
            "Organization",
            "PractitionerRole"
        )
        val numberOfResources = supportedResources.size
        assertEquals(numberOfResources, LoadSpringConfig().loadTopics().size)
        assertEquals(2 * numberOfResources, PublishSpringConfig().publishTopics().size)
    }
}
