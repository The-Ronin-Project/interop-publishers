package com.projectronin.interop.aidbox.spring

import com.projectronin.interop.aidbox.AidboxPublishService
import com.projectronin.interop.aidbox.ConceptMapService
import com.projectronin.interop.aidbox.LocationService
import com.projectronin.interop.aidbox.PatientService
import com.projectronin.interop.aidbox.PractitionerService
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [AidboxSpringConfig::class])
class AidboxSpringConfigTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `loads AidboxPublishService`() {
        val service = applicationContext.getBean<AidboxPublishService>()
        assertNotNull(service)
        assertInstanceOf(AidboxPublishService::class.java, service)
    }

    @Test
    fun `loads ConceptMapService`() {
        val service = applicationContext.getBean<ConceptMapService>()
        assertNotNull(service)
        assertInstanceOf(ConceptMapService::class.java, service)
    }

    @Test
    fun `loads LocationService`() {
        val service = applicationContext.getBean<LocationService>()
        assertNotNull(service)
        assertInstanceOf(LocationService::class.java, service)
    }

    @Test
    fun `loads PatientService`() {
        val service = applicationContext.getBean<PatientService>()
        assertNotNull(service)
        assertInstanceOf(PatientService::class.java, service)
    }

    @Test
    fun `loads PractitionerService`() {
        val service = applicationContext.getBean<PractitionerService>()
        assertNotNull(service)
        assertInstanceOf(PractitionerService::class.java, service)
    }
}
