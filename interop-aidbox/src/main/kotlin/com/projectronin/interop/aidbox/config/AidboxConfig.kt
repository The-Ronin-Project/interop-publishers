package com.projectronin.interop.aidbox.config

import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan("com.projectronin.interop.aidbox")
class AidboxConfig {
    @Bean("AidboxHTTPClient")
    fun getHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(JsonFeature) {
                serializer = JacksonSerializer() {
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                }
            }
        }
    }
}
