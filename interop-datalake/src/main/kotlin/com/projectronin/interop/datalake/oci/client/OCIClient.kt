package com.projectronin.interop.datalake.oci.client

import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.projectronin.interop.datalake.oci.auth.OCIConfiguration
import java.io.ByteArrayInputStream

/***
 * Client for interactive with OCI's object store
 */
class OCIClient(
    private val config: OCIConfiguration
) {
    private val client by lazy { ObjectStorageClient(config.getAuthentication()) }

    /**
     * Upload the string found in [data] to [fileName]
     */
    fun upload(fileName: String, data: String) {
        val putObjectRequest = PutObjectRequest.builder()
            .objectName(fileName)
            .putObjectBody(ByteArrayInputStream(data.toByteArray()))
            .namespaceName(config.nameSpace)
            .bucketName(config.bucketName)
            .build()
        client.putObject(putObjectRequest)
    }

    /**
     * Retrieves the contents of the object found at [fileName]
     */
    fun getObjectBody(fileName: String): String? {
        val getObjectRequest = GetObjectRequest.builder()
            .objectName(fileName)
            .namespaceName(config.nameSpace)
            .bucketName(config.bucketName)
            .build()
        val inputStream = client.getObject(getObjectRequest).inputStream
        return inputStream?.bufferedReader().use { it?.readText() }
    }
}
