package com.projectronin.interop.datalake.azure.client

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClientBuilder
import com.projectronin.interop.datalake.azure.auth.AzureAuthenticationBroker
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Client for uploading to a datalake container
 */
@Component
data class AzureClient(
    @Value("\${datalake.azure.blob.endpoint}")
    private val blobEndpoint: String,
    @Value("\${datalake.azure.container}")
    private val container: String,
    private val authenticationBroker: AzureAuthenticationBroker
) {

    fun upload(fileName: String, data: String) {
        val authentication = authenticationBroker.getAuthentication()
        val blobClient = BlobClientBuilder()
            .endpoint(blobEndpoint)
            .containerName(container)
            .sasToken(authentication.accessToken)
            .blobName(fileName)
            .buildClient()

        // Upload the resource
        blobClient.upload(BinaryData.fromString(data), true)
    }
}
