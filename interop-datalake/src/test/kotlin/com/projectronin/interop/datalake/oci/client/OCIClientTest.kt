package com.projectronin.interop.datalake.oci.client

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.PutObjectResponse
import com.projectronin.interop.datalake.oci.auth.OCIConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OCIClientTest {
    private val credentials = mockk<OCIConfiguration> {
        every { nameSpace } returns "nameSpace"
        every { bucketName } returns "bucket"
    }

    @Test
    fun `getContentBody - works`() {
        val mockRequest = mockk<GetObjectRequest> {}
        mockkConstructor(GetObjectRequest.Builder::class)
        val mockBuilder = mockk<GetObjectRequest.Builder> {
            every { namespaceName("nameSpace") } returns this
            every { bucketName("bucket") } returns this
            every { build() } returns mockRequest
        }
        every { anyConstructed<GetObjectRequest.Builder>().objectName("test") } returns mockBuilder

        val mockResponse = mockk<GetObjectResponse> {
            every { inputStream } returns "blag".byteInputStream()
        }
        val mockObjectStorageClient = mockk<ObjectStorageClient> {
            every { getObject(mockRequest) } returns mockResponse
        }

        val client = spyk(OCIClient(credentials))
        every { client getProperty "client" } returns mockObjectStorageClient
        every { client.getObjectBody("test") } answers { callOriginal() }
        assertEquals("blag", client.getObjectBody("test"))
    }

    @Test
    fun `getContentBody - works with null`() {
        val mockRequest = mockk<GetObjectRequest> {}
        mockkConstructor(GetObjectRequest.Builder::class)
        val mockBuilder = mockk<GetObjectRequest.Builder> {
            every { namespaceName("nameSpace") } returns this
            every { bucketName("bucket") } returns this
            every { build() } returns mockRequest
        }
        every { anyConstructed<GetObjectRequest.Builder>().objectName("test") } returns mockBuilder

        val mockResponse = mockk<GetObjectResponse> {
            every { inputStream } returns null
        }
        val mockObjectStorageClient = mockk<ObjectStorageClient> {
            every { getObject(mockRequest) } returns mockResponse
        }

        val client = spyk(OCIClient(credentials))
        every { client getProperty "client" } returns mockObjectStorageClient
        every { client.getObjectBody("test") } answers { callOriginal() }
        assertNull(client.getObjectBody("test"))
    }

    @Test
    fun `put object - works`() {
        val mockRequest = mockk<PutObjectRequest> {}
        mockkConstructor(PutObjectRequest.Builder::class)
        val mockBuilder = mockk<PutObjectRequest.Builder> {
            every { namespaceName("nameSpace") } returns this
            every { bucketName("bucket") } returns this
            every { putObjectBody(any()) } returns this
            every { build() } returns mockRequest
        }
        every { anyConstructed<PutObjectRequest.Builder>().objectName("test") } returns mockBuilder

        val mockResponse = mockk<PutObjectResponse> {}
        val mockObjectStorageClient = mockk<ObjectStorageClient> {
            every { putObject(mockRequest) } returns mockResponse
        }

        val client = spyk(OCIClient(credentials))
        every { client getProperty "client" } returns mockObjectStorageClient
        every { client.upload("test", "content") } answers { callOriginal() }
        assertNotNull(client.upload("test", "content"))
    }

    @AfterEach
    fun `unmockk all`() {
        unmockkAll()
    }
}
