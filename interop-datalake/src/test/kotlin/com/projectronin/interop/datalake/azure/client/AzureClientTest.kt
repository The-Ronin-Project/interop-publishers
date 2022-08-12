package com.projectronin.interop.datalake.azure.client

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobClientBuilder
import com.projectronin.interop.datalake.azure.auth.AzureAuthenticationBroker
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AzureClientTest {
    private val mockEndpoint = "mockEndpoint"
    private val mockContainer = "mockContainer"
    private val client = mockk<AzureAuthenticationBroker> {
        every { getAuthentication() } returns mockk {
            every { accessToken } returns "mockAccessToken"
        }
    }
    @Test
    fun `upload works`() {
        val fileName = "test.wav.jpg"
        val data = "garbage!"

        // function builds binary data, mock that
        mockkStatic(BinaryData::class)
        val mockBinaryData = mockk<BinaryData> {}
        every { BinaryData.fromString(data) } returns mockBinaryData
        val mockBlobClient = mockk<BlobClient> {
            every { upload(mockBinaryData, true) } returns Unit
        }

        // mocks the constructed client
        mockkConstructor(BlobClientBuilder::class)
        val mockBlobClientContainerBuilder = mockk<BlobClientBuilder> {
            every { containerName(mockContainer) } returns this
            every { sasToken("mockAccessToken") } returns this
            every { blobName(fileName) } returns this
            every { buildClient() } returns mockBlobClient
        }
        every { anyConstructed<BlobClientBuilder>().endpoint(mockEndpoint) } returns mockBlobClientContainerBuilder

        val client = AzureClient(mockEndpoint, mockContainer, client)
        client.upload(fileName, data)
        assertTrue(true)
    }

    @AfterEach
    fun unmock() {
        unmockkAll()
    }
}
