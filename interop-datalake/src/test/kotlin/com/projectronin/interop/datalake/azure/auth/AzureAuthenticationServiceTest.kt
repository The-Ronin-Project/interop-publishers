package com.projectronin.interop.datalake.azure.auth

import com.azure.identity.AzureAuthorityHosts
import com.azure.identity.ClientSecretCredential
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.UserDelegationKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AzureAuthenticationServiceTest {
    private val mockEndpoint = "mockEndpoint"
    private val mockAzureTenantId = "mockTenantId"
    private val mockContainer = "mockContainer"
    private val client = mockk<AzureCredentials> {
        every { clientId } returns "mockClientId"
        every { clientSecret } returns "mockClientSecret"
    }

    @Test
    fun `authentication works`() {
        val authority = AzureAuthorityHosts.AZURE_PUBLIC_CLOUD + mockAzureTenantId
        mockkConstructor(ClientSecretCredentialBuilder::class)
        mockkConstructor(BlobServiceClientBuilder::class)

        val mockkedClientSecretCredentials = mockk<ClientSecretCredential> {}
        val mockkedClientSecretBuilder = mockk<ClientSecretCredentialBuilder> {
            every { tenantId(mockAzureTenantId) } returns this
            every { clientId("mockClientId") } returns this
            every { clientSecret("mockClientSecret") } returns this
            every { build() } returns mockkedClientSecretCredentials
        }
        every { anyConstructed<ClientSecretCredentialBuilder>().authorityHost(authority) } returns mockkedClientSecretBuilder

        val mockUserDelegationKey = mockk<UserDelegationKey>()
        val mockContainerClient = mockk<BlobContainerClient>() {
            every { generateUserDelegationSas(any(), mockUserDelegationKey) } returns "fake token"
        }
        val mockBlobServiceClient = mockk<BlobServiceClient> {
            every { getUserDelegationKey(any(), any()) } returns mockUserDelegationKey
            every { getBlobContainerClient(mockContainer) } returns mockContainerClient
        }
        val mockkedBlobServiceClientBuilder = mockk<BlobServiceClientBuilder> {
            every { credential(mockkedClientSecretCredentials) } returns this
            every { buildClient() } returns mockBlobServiceClient
        }
        every { anyConstructed<BlobServiceClientBuilder>().endpoint(mockEndpoint) } returns mockkedBlobServiceClientBuilder

        val authService = AzureAuthenticationService(mockEndpoint, mockAzureTenantId, mockContainer, client)
        val authentication = authService.getAuthentication()
        assertEquals("fake token", authentication.accessToken)
        assertNotNull(authentication.expiresAt)
    }

    @AfterEach
    fun unmock() {
        unmockkAll()
    }
}
