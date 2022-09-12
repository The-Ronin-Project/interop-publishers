package com.projectronin.interop.publishers

import com.projectronin.interop.aidbox.utils.RONIN_TENANT_SYSTEM
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.publishers.exception.AidboxPublishException
import com.projectronin.interop.publishers.exception.DatalakePublishException
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.projectronin.interop.aidbox.PublishService as AidboxPublishService
import com.projectronin.interop.datalake.PublishService as DatalakePublishService

class PublishServiceTest {
    private lateinit var aidboxPublishService: AidboxPublishService
    private lateinit var datalakePublishService: DatalakePublishService
    private lateinit var service: PublishService

    private val tenantId = "tenant"

    private val roninDomainResources = listOf(
        Practitioner(
            id = Id("cmjones"),
            identifier = listOf(
                Identifier(system = Uri(RONIN_TENANT_SYSTEM), value = "third")
            ),
            name = listOf(
                HumanName(family = "Jones", given = listOf("Cordelia", "May"))
            )
        ),
        Practitioner(
            id = Id("rallyr"),
            identifier = listOf(
                Identifier(system = Uri(RONIN_TENANT_SYSTEM), value = "second")
            ),
            name = listOf(
                HumanName(
                    family = "Llyr",
                    given = listOf("Regan", "Anne")
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
    fun `publishes FHIR resources to Aidbox and datalake`() {
        every { aidboxPublishService.publish(roninDomainResources) } returns true
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } just runs

        assertTrue(service.publishFHIRResources(tenantId, roninDomainResources))
    }

    @Test
    fun `handles aidbox failing to publish FHIR resources`() {
        every { aidboxPublishService.publish(roninDomainResources) } returns false

        assertThrows<AidboxPublishException> {
            service.publishFHIRResources(tenantId, roninDomainResources)
        }
    }

    @Test
    fun `handles publish to datalake failing`() {
        val exception = IllegalStateException("exception")

        every { aidboxPublishService.publish(roninDomainResources) } returns true
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } throws exception

        val e = assertThrows<DatalakePublishException> {
            service.publishFHIRResources(tenantId, roninDomainResources)
        }

        assertEquals(exception, e.cause)
    }
}
