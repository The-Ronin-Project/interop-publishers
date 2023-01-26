package com.projectronin.interop.kafka.spring

import com.projectronin.interop.kafka.model.LoadTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoadSpringConfig {

    @Bean
    fun loadTopics(): List<LoadTopic> {
        val system = "interop-platform"

        return listOf(
            LoadTopic(
                systemName = system,
                topicName = "azure.centralus.interop-platform.patient-load.v1",
                dataSchema = "https://github.com/projectronin/contract-event-interop-resource-load/blob/main/v1/resource-load-v1.schema.json",
                resourceType = "Patient"
            )
        )
    }
}
