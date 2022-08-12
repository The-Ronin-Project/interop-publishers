package com.projectronin.interop.datalake.azure.auth

import com.azure.identity.AzureAuthorityHosts
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.sas.BlobContainerSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

/**
 * Service providing Authentication capabilities for an Azure Datalake [container] instance located at [blobEndpoint] with the provided [azureCredentials].
 */
@Service
class AzureAuthenticationService(
    @Value("\${datalake.azure.blob.endpoint}")
    private val blobEndpoint: String,
    @Value("\${datalake.azure.tenant.id}")
    private val dataLakeTenantId: String,
    @Value("\${datalake.azure.container}")
    private val container: String,
    private val azureCredentials: AzureCredentials
) {
    private val blobContainerSasPermissions = BlobContainerSasPermission().setWritePermission(true)
    private val azureAuthorityUrl = AzureAuthorityHosts.AZURE_PUBLIC_CLOUD + dataLakeTenantId
    private val credentials = ClientSecretCredentialBuilder()
        .authorityHost(azureAuthorityUrl)
        .tenantId(dataLakeTenantId)
        .clientId(azureCredentials.clientId)
        .clientSecret(azureCredentials.clientSecret)
        .build()
    private val blobServiceClient = BlobServiceClientBuilder()
        .endpoint(blobEndpoint)
        .credential(credentials)
        .buildClient()

    /**
     * Retrieves an Authentication.
     */
    fun getAuthentication(): AzureAuthentication {
        val keyExpiry = OffsetDateTime.now().plusDays(1)
        val keyStart = OffsetDateTime.now()
        val blobServiceSignatureValues = BlobServiceSasSignatureValues(keyExpiry, blobContainerSasPermissions)
            .setStartTime(keyStart)

        val userDelegationKey = blobServiceClient.getUserDelegationKey(keyStart, keyExpiry)
        val blobContainerClient = blobServiceClient.getBlobContainerClient(container)

        val token = blobContainerClient.generateUserDelegationSas(blobServiceSignatureValues, userDelegationKey)

        return AzureAuthentication(accessToken = token, expiresAt = keyExpiry.toInstant())
    }
}
