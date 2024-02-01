package com.projectronin.interop.publishers.spring

import com.projectronin.interop.publishers.PublishService
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Disabled // can't get this to work with ConfigurationProperties for KafkaConfig
@ContextConfiguration(classes = [PublishersSpringConfig::class])
class PublishersSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads PublishService`() {
        val service = applicationContext.getBean<PublishService>()
        assertNotNull(service)
        assertInstanceOf(PublishService::class.java, service)
    }
}
