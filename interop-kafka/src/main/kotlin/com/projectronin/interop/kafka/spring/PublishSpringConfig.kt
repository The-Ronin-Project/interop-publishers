package com.projectronin.interop.kafka.spring

import com.projectronin.event.interop.resource.publish.v1.InteropResourcePublishV1
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.PublishTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PublishSpringConfig {

    @Bean
    fun publishTopics(): List<PublishTopic> {
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
        return supportedResources.map {
            generateTopics(it)
        }.flatten()
    }

    fun generateTopics(resourceType: String): List<PublishTopic> {
        val system = "interop-mirth"
        val nightlyTopic = PublishTopic(
            systemName = system,
            topicName = "oci.us-phoenix-1.interop-mirth.${resourceType.lowercase()}-publish-nightly.v1",
            dataSchema = "https://github.com/projectronin/contract-event-interop-resource-publish/blob/main/v1/resource-publish-v1.schema.json",
            resourceType = resourceType,
            dataTrigger = DataTrigger.NIGHTLY,
            converter = { tenant, resource ->
                InteropResourcePublishV1(
                    tenantId = tenant,
                    resourceJson = objectMapper.writeValueAsString(resource),
                    resourceType = resource.resourceType,
                    dataTrigger = InteropResourcePublishV1.DataTrigger.nightly
                )
            }
        )

        val adHocTopic = PublishTopic(
            systemName = system,
            topicName = "oci.us-phoenix-1.interop-mirth.${resourceType.lowercase()}-publish-adhoc.v1",
            dataSchema = "https://github.com/projectronin/contract-event-interop-resource-publish/blob/main/v1/resource-publish-v1.schema.json",
            resourceType = resourceType,
            dataTrigger = DataTrigger.AD_HOC,
            converter = { tenant, resource ->
                InteropResourcePublishV1(
                    tenantId = tenant,
                    resourceJson = objectMapper.writeValueAsString(resource),
                    resourceType = resource.resourceType,
                    dataTrigger = InteropResourcePublishV1.DataTrigger.adhoc
                )
            }
        )

        return listOf(nightlyTopic, adHocTopic)
    }
}
