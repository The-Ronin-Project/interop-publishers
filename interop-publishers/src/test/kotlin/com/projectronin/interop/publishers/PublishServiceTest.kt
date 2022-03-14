package com.projectronin.interop.publishers

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPractitioner
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.projectronin.interop.aidbox.PublishService as AidboxPublishService

class PublishServiceTest {
    private lateinit var aidboxPublishService: AidboxPublishService
    private lateinit var service: PublishService

    private val roninDomainResources = listOf(
        OncologyPractitioner(
            id = Id("cmjones"),
            identifier = listOf(
                Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.RONIN_TENANT, value = "third")
            ),
            name = listOf(
                HumanName(family = "Jones", given = listOf("Cordelia", "May"))
            )
        ),
        OncologyPractitioner(
            id = Id("rallyr"),
            identifier = listOf(
                Identifier(system = CodeSystem.RONIN_TENANT.uri, type = CodeableConcepts.RONIN_TENANT, value = "second")
            ),
            name = listOf(
                HumanName(
                    family = "Llyr", given = listOf("Regan", "Anne")
                )
            )
        )
    )
    // Needs more (or less)

    @BeforeEach
    fun setup() {
        aidboxPublishService = mockk()

        service = PublishService(aidboxPublishService)
    }

    @Test
    fun `publishes FHIR resources to Aidbox`() {
        every { aidboxPublishService.publish(roninDomainResources) } returns true

        assertTrue(service.publishFHIRResources(roninDomainResources))
    }

    @Test
    fun `handles aidbox failing to publish FHIR resources`() {
        every { aidboxPublishService.publish(roninDomainResources) } returns false

        assertFalse(service.publishFHIRResources(roninDomainResources))
    }
}
