package com.projectronin.interop.kafka.spring

import com.projectronin.interop.kafka.model.LoadTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoadSpringConfig(private val kafkaSpringConfig: KafkaConfig) {

    @Bean
    fun loadTopics(): List<LoadTopic> {
        val supportedResources = listOf(
            "Patient",
            "Binary",
            "Practitioner",
            "Appointment",
            "CarePlan",
            "CareTeam",
            "Communication",
            "Condition",
            "Encounter",
            "DocumentReference",
            "Location",
            "Medication",
            "MedicationRequest",
            "MedicationStatement",
            "Observation",
            "Organization",
            "PractitionerRole"
        )
        return supportedResources.map {
            generateTopics(it)
        }
    }

    fun generateTopics(resourceType: String): LoadTopic {
        val topicParameters = listOf(kafkaSpringConfig.cloud.vendor, kafkaSpringConfig.cloud.region, "interop-mirth", "${resourceType.lowercase()}-load", "v1")
        return LoadTopic(
            systemName = kafkaSpringConfig.retrieve.serviceId,
            topicName = topicParameters.joinToString("."),
            dataSchema = "https://github.com/projectronin/contract-event-interop-resource-load/blob/main/v1/resource-load-v1.schema.json",
            resourceType = resourceType
        )
    }
}
