package com.projectronin.interop.datalake.oci.client

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.projectronin.interop.datalake.oci.auth.OCICredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

/***
 * Client for interactive with OCI's object store
 */
@Component
class OCIClient(
    @Value("\${datalake.oci.nameSpace}")
    private val nameSpace: String,
    @Value("\${datalake.oci.bucketName}")
    private val bucketName: String,
    private val credentials: OCICredentials
) {
    val client by lazy { ObjectStorageClient(credentials.getAuthentication()) }

    /**
     * Upload the string found in [data] to [fileName]
     */
    fun upload(fileName: String, data: String) {
        val putObjectRequest = PutObjectRequest.builder()
            .objectName(fileName)
            .putObjectBody(ByteArrayInputStream(data.toByteArray()))
            .namespaceName(nameSpace)
            .bucketName(bucketName)
            .build()
        client.putObject(putObjectRequest)
    }

    /**
     * Retrieves the contents of the object found at [fileName]
     */
    fun getObjectBody(fileName: String): String? {
        val getObjectRequest = GetObjectRequest.builder()
            .objectName(fileName)
            .namespaceName(nameSpace)
            .bucketName(bucketName)
            .build()
        val inputStream = client.getObject(getObjectRequest).inputStream
        return inputStream?.bufferedReader().use { it?.readText() }
    }
}
