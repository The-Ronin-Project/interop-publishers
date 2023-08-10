package com.projectronin.interop.publishers

import com.projectronin.ehr.dataauthority.client.EHRDataAuthorityClient
import com.projectronin.ehr.dataauthority.models.BatchResourceResponse
import com.projectronin.ehr.dataauthority.models.FailedResource
import com.projectronin.ehr.dataauthority.models.ModificationType
import com.projectronin.ehr.dataauthority.models.SucceededResource
import com.projectronin.event.interop.internal.v1.Metadata
import com.projectronin.interop.datalake.DatalakePublishService
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.kafka.KafkaPublishService
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.publishers.exception.PublishException
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PublishServiceTest {
    private lateinit var ehrDataAuthorityClient: EHRDataAuthorityClient
    private lateinit var datalakePublishService: DatalakePublishService
    private lateinit var kafkaPublishService: KafkaPublishService
    private lateinit var service: PublishService

    private val tenantId = "tenant"
    private val metadata = mockk<Metadata>()

    private val practitioner1Id = "cmjones"
    private val practitioner1 = Practitioner(
        id = Id(practitioner1Id),
        identifier = listOf(
            Identifier(system = CodeSystem.RONIN_TENANT.uri, value = "third".asFHIR())
        ),
        name = listOf(
            HumanName(family = "Jones".asFHIR(), given = listOf("Cordelia", "May").asFHIR())
        )
    )

    private val practitioner2Id = "rallyr"
    private val practitioner2 = Practitioner(
        id = Id(practitioner2Id),
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
    private val roninDomainResources = listOf(practitioner1, practitioner2)

    @BeforeEach
    fun setup() {
        ehrDataAuthorityClient = mockk()
        datalakePublishService = mockk()
        kafkaPublishService = mockk()

        service = PublishService(ehrDataAuthorityClient, datalakePublishService, kafkaPublishService)
    }

    @Test
    fun `publishes FHIR resources to EHR Data Authority`() {
        coEvery { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) } returns BatchResourceResponse(
            succeeded = listOf(
                SucceededResource("Practitioner", practitioner1Id, ModificationType.CREATED),
                SucceededResource("Practitioner", practitioner2Id, ModificationType.UPDATED)
            )
        )
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } just runs

        assertTrue(service.publishFHIRResources(tenantId, roninDomainResources, metadata))

        coVerify(exactly = 1) { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) }
        verify(exactly = 1) { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) }
        verify { kafkaPublishService wasNot Called }
    }

    @Test
    fun `publishes FHIR resources to Kafka`() {
        coEvery { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) } returns BatchResourceResponse(
            succeeded = listOf(
                SucceededResource("Practitioner", practitioner1Id, ModificationType.CREATED),
                SucceededResource("Practitioner", practitioner2Id, ModificationType.UPDATED)
            )
        )
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } just runs
        every {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        } returns mockk()

        assertTrue(service.publishFHIRResources(tenantId, roninDomainResources, metadata, DataTrigger.AD_HOC))

        coVerify(exactly = 1) { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) }
        verify(exactly = 1) { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) }
        verify(exactly = 1) {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        }
    }

    @Test
    fun `handles partial failure of EHR Data Authority publication`() {
        coEvery { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) } returns BatchResourceResponse(
            succeeded = listOf(
                SucceededResource("Practitioner", practitioner1Id, ModificationType.CREATED)
            ),
            failed = listOf(
                FailedResource("Practitioner", practitioner2Id, "Error!")
            )
        )
        every { datalakePublishService.publishFHIRR4(tenantId, listOf(practitioner1)) } just runs
        every {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                listOf(practitioner1),
                metadata
            )
        } returns mockk()

        val exception = assertThrows<PublishException> {
            service.publishFHIRResources(tenantId, roninDomainResources, metadata, DataTrigger.AD_HOC)
        }
        assertEquals(
            "Published 1 resources, but failed to publish 1 resources: \n" +
                "Practitioner/$practitioner2Id: Error!",
            exception.message
        )

        coVerify(exactly = 1) { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) }
        verify(exactly = 1) { datalakePublishService.publishFHIRR4(tenantId, listOf(practitioner1)) }
        verify(exactly = 1) {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                listOf(practitioner1),
                metadata
            )
        }
    }

    @Test
    fun `handles complete failure of EHR Data Authority publication`() {
        coEvery { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) } returns BatchResourceResponse(
            failed = listOf(
                FailedResource("Practitioner", practitioner1Id, "First Error!"),
                FailedResource("Practitioner", practitioner2Id, "Second Error!")
            )
        )

        val exception = assertThrows<PublishException> {
            service.publishFHIRResources(tenantId, roninDomainResources, metadata, DataTrigger.AD_HOC)
        }
        assertEquals(
            "Published 0 resources, but failed to publish 2 resources: \n" +
                "Practitioner/$practitioner1Id: First Error!\n" +
                "Practitioner/$practitioner2Id: Second Error!",
            exception.message
        )

        coVerify(exactly = 1) { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) }
        verify { datalakePublishService wasNot Called }
        verify { kafkaPublishService wasNot Called }
    }

    @Test
    fun `publishes new FHIR resources to Datalake`() {
        coEvery { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) } returns BatchResourceResponse(
            succeeded = listOf(
                SucceededResource("Practitioner", practitioner1Id, ModificationType.CREATED),
                SucceededResource("Practitioner", practitioner2Id, ModificationType.CREATED)
            )
        )
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } just runs
        every {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        } returns mockk()

        assertTrue(service.publishFHIRResources(tenantId, roninDomainResources, metadata, DataTrigger.AD_HOC))

        coVerify(exactly = 1) { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) }
        verify(exactly = 1) { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) }
        verify(exactly = 1) {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        }
    }

    @Test
    fun `publishes updated FHIR resources to Datalake`() {
        coEvery { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) } returns BatchResourceResponse(
            succeeded = listOf(
                SucceededResource("Practitioner", practitioner1Id, ModificationType.UPDATED),
                SucceededResource("Practitioner", practitioner2Id, ModificationType.UPDATED)
            )
        )
        every { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) } just runs
        every {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        } returns mockk()

        assertTrue(service.publishFHIRResources(tenantId, roninDomainResources, metadata, DataTrigger.AD_HOC))

        coVerify(exactly = 1) { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) }
        verify(exactly = 1) { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) }
        verify(exactly = 1) {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        }
    }

    @Test
    fun `does not publish unmodified FHIR resources to Datalake`() {
        coEvery { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) } returns BatchResourceResponse(
            succeeded = listOf(
                SucceededResource("Practitioner", practitioner1Id, ModificationType.UNMODIFIED),
                SucceededResource("Practitioner", practitioner2Id, ModificationType.UNMODIFIED)
            )
        )
        every {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        } returns mockk()

        assertTrue(service.publishFHIRResources(tenantId, roninDomainResources, metadata, DataTrigger.AD_HOC))

        coVerify(exactly = 1) { ehrDataAuthorityClient.addResources(tenantId, roninDomainResources) }
        verify(exactly = 0) { datalakePublishService.publishFHIRR4(tenantId, roninDomainResources) }
        verify(exactly = 1) {
            kafkaPublishService.publishResources(
                tenantId,
                DataTrigger.AD_HOC,
                roninDomainResources,
                metadata
            )
        }
    }
}
