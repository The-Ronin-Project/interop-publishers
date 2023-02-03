package com.projectronin.interop.datalake.oci.client

import com.oracle.bmc.Region
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Base64
import java.util.function.Supplier

/***
 * Client for interactive with OCI's object store
 */
@Component
class OCIClient(
    @Value("\${oci.tenancy.ocid}")
    private val tenancyOCID: String,
    @Value("\${oci.user.ocid}")
    private val userOCID: String,
    @Value("\${oci.fingerPrint}")
    private val fingerPrint: String,
    @Value("\${oci.private.key.base64}")
    private val privateKey: String,
    @Value("\${oci.namespace}")
    private val namespace: String,
    @Value("\${oci.conceptmap.bucket.name}")
    private val infxBucket: String,
    @Value("\${oci.publish.bucket.name}")
    private val datalakeBucket: String,
    @Value("\${oci.region}")
    private val regionId: String = "us-phoenix-1"
) {
    private val privateKeySupplier: Supplier<InputStream> = Supplier<InputStream> { Base64.getDecoder().decode(privateKey).inputStream() }
    val authProvider: SimpleAuthenticationDetailsProvider by lazy {
        SimpleAuthenticationDetailsProvider.builder()
            .tenantId(tenancyOCID)
            .userId(userOCID)
            .fingerprint(fingerPrint)
            .region(Region.fromRegionId(regionId))
            .privateKeySupplier(privateKeySupplier)
            .build()
    }
    private val client by lazy { ObjectStorageClient(authProvider) }

    /**
     * Retrieves the contents of the object found at [fileName] in the infx-shared bucket
     */
    fun getObjectFromINFX(fileName: String): String? {
        return getObjectBody(infxBucket, fileName)
    }

    /**
     * Upload the string found in [data] to [fileName] to the datalake bucket
     *  Returns true if it was successful
     */
    fun uploadToDatalake(fileName: String, data: String): Boolean {
        return upload(datalakeBucket, fileName, data)
    }

    /**
     * Upload the string found in [data] to [fileName]
     * Returns true if it was successful
     */
    fun upload(bucket: String, fileName: String, data: String): Boolean {
        val putObjectRequest = PutObjectRequest.builder()
            .objectName(fileName)
            .putObjectBody(ByteArrayInputStream(data.toByteArray()))
            .namespaceName(namespace)
            .bucketName(bucket)
            .build()

        // OCI JDK natively supports retrying, but errors occasionally when something unexpected happens
        // https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/javasdkconcepts.htm

        val responseStatusCode = try {
            client.putObject(putObjectRequest).__httpStatusCode__
            // client side exception in the OCI JDK
        } catch (bmcException: BmcException) {
            if (bmcException.statusCode == -1) {
                runBlocking { delay(5000) }
                client.putObject(putObjectRequest).__httpStatusCode__
            } else {
                null
            }
        }
        return responseStatusCode in (200..202)
    }

    /**
     * Retrieves the contents of the object found at [fileName]
     */
    fun getObjectBody(bucket: String, fileName: String): String? {
        val getObjectRequest = GetObjectRequest.builder()
            .objectName(fileName)
            .namespaceName(namespace)
            .bucketName(bucket)
            .build()
        val inputStream = client.getObject(getObjectRequest).inputStream
        return inputStream?.bufferedReader().use { it?.readText() }
    }
}
