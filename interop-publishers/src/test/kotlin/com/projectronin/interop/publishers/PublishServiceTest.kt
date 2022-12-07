package com.projectronin.interop.publishers

import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.publishers.exception.AidboxPublishException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.aidbox.PublishService as AidboxPublishService

class PublishServiceTest {
    private lateinit var aidboxPublishService: AidboxPublishService
    private lateinit var datalakePublishService: DatalakePublishService
    private lateinit var service: PublishService

    private val tenantId = "tenant"

    private val roninDomainResources = listOf(
        Practitioner(
            id = Id("cmjones"),
            identifier = listOf(
                Identifier(system = CodeSystem.RONIN_TENANT.uri, value = "third".asFHIR())
            ),
            name = listOf(
                HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR())
            )
        ),
        Practitioner(
            id = Id("rallyr"),
            identifier = listOf(
                Identifier(system = CodeSystem.RONIN_TENANT.uri, value = "second".asFHIR())
            ),
            name = listOf(
                HumanName(
                    family = "Llyr".asFHIR(),
                    given = listOf("Regan", "Anne").asFHIR()
                )
            )
        )
    )

    @BeforeEach
    fun setup() {
        aidboxPublishService = mockk()
        datalakePublishService = mockk()

        service = PublishService(aidboxPublishService, datalakePublishService)
    }

    @Test
    fun `publishes FHIR resources to Aidbox`() {
        every { aidboxPublishService.publish(roninDomainResources) } returns true
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } just runs

        assertTrue(service.publishFHIRResources(tenantId, roninDomainResources))
    }

    @Test
    fun `handles aidbox failing to publish FHIR resources`() {
        every { aidboxPublishService.publish(roninDomainResources) } returns false
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } just runs

        assertThrows<AidboxPublishException> {
            service.publishFHIRResources(tenantId, roninDomainResources)
        }
    }
}
