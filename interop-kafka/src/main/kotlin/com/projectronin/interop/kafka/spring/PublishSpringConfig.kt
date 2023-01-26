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
        val system = "interop-platform"

        return listOf(
            PublishTopic(
                systemName = system,
                topicName = "azure.centralus.interop-platform.patient-publish-nightly.v1",
                dataSchema = "https://github.com/projectronin/contract-event-interop-resource-publish/blob/main/v1/resource-publish-v1.schema.json",
                resourceType = "Patient",
                dataTrigger = DataTrigger.NIGHTLY,
                converter = { tenant, resource ->
                    InteropResourcePublishV1(
                        tenantId = tenant,
                        resourceJson = objectMapper.writeValueAsString(resource),
                        resourceType = resource.resourceType,
                        dataTrigger = InteropResourcePublishV1.DataTrigger.nightly
                    )
                }
            ),
            PublishTopic(
                systemName = system,
                topicName = "azure.centralus.interop-platform.patient-publish-adhoc.v1",
                dataSchema = "https://github.com/projectronin/contract-event-interop-resource-publish/blob/main/v1/resource-publish-v1.schema.json",
                resourceType = "Patient",
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
        )
    }
}
