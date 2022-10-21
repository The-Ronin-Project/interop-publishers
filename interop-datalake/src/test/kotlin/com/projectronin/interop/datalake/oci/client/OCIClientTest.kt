package com.projectronin.interop.datalake.oci.client

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.PutObjectResponse
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
import java.util.Base64

class OCIClientTest {
    private val testClient = OCIClient(
        "tenancy",
        "user",
        "fingerprint",
        "key",
        "namespace",
        "infxbucket",
        "datalakebucket",
        "region"
    )

    @Test
    fun `getAuthentication - works`() {
        mockkConstructor(SimpleAuthenticationDetailsProviderBuilder::class)
        val privateString = "-----BEGIN PRIVATE KEY-----\n" +
            "-----END PRIVATE KEY-----"
        val credentials = OCIClient(
            tenancyOCID = "ocid1.tenancy.oc1",
            userOCID = "ocid1.user.oc1.",
            fingerPrint = "a1:",
            privateKey = Base64.getEncoder().encodeToString(privateString.toByteArray()),
            namespace = "Namespace",
            infxBucket = "iBucket",
            datalakeBucket = "dBucket"
        )
        val auth = credentials.authProvider

        assertNotNull(auth)

        assertNotNull(auth.privateKey)
        unmockkAll()
    }

    @Test
    fun `getContentBody - works`() {
        val mockRequest = mockk<GetObjectRequest> {}
        mockkConstructor(GetObjectRequest.Builder::class)
        val mockBuilder = mockk<GetObjectRequest.Builder> {
            every { namespaceName("namespace") } returns this
            every { bucketName("infxbucket") } returns this
            every { build() } returns mockRequest
        }
        every { anyConstructed<GetObjectRequest.Builder>().objectName("test") } returns mockBuilder

        val mockResponse = mockk<GetObjectResponse> {
            every { inputStream } returns "blag".byteInputStream()
        }
        val mockObjectStorageClient = mockk<ObjectStorageClient> {
            every { getObject(mockRequest) } returns mockResponse
        }

        val client = spyk(testClient)
        every { client getProperty "client" } returns mockObjectStorageClient
        every { client.getObjectFromINFX("test") } answers { callOriginal() }
        assertEquals("blag", client.getObjectFromINFX("test"))
    }

    @Test
    fun `getContentBody - works with null`() {
        val mockRequest = mockk<GetObjectRequest> {}
        mockkConstructor(GetObjectRequest.Builder::class)
        val mockBuilder = mockk<GetObjectRequest.Builder> {
            every { namespaceName("namespace") } returns this
            every { bucketName("infxbucket") } returns this
            every { build() } returns mockRequest
        }
        every { anyConstructed<GetObjectRequest.Builder>().objectName("test") } returns mockBuilder

        val mockResponse = mockk<GetObjectResponse> {
            every { inputStream } returns null
        }
        val mockObjectStorageClient = mockk<ObjectStorageClient> {
            every { getObject(mockRequest) } returns mockResponse
        }

        val client = spyk(testClient)
        every { client getProperty "client" } returns mockObjectStorageClient
        every { client.getObjectFromINFX("test") } answers { callOriginal() }
        assertNull(client.getObjectFromINFX("test"))
    }

    @Test
    fun `put object - works`() {
        val mockRequest = mockk<PutObjectRequest> {}
        mockkConstructor(PutObjectRequest.Builder::class)
        val mockBuilder = mockk<PutObjectRequest.Builder> {
            every { namespaceName("namespace") } returns this
            every { bucketName("datalakebucket") } returns this
            every { putObjectBody(any()) } returns this
            every { build() } returns mockRequest
        }
        every { anyConstructed<PutObjectRequest.Builder>().objectName("test") } returns mockBuilder

        val mockResponse = mockk<PutObjectResponse> {}
        val mockObjectStorageClient = mockk<ObjectStorageClient> {
            every { putObject(mockRequest) } returns mockResponse
        }

        val client = spyk(testClient)
        every { client getProperty "client" } returns mockObjectStorageClient
        every { client.uploadToDatalake("test", "content") } answers { callOriginal() }
        assertNotNull(client.uploadToDatalake("test", "content"))
    }

    @AfterEach
    fun `unmockk all`() {
        unmockkAll()
    }
}
