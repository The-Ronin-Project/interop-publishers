package com.projectronin.interop.datalake.oci.auth

import com.oracle.bmc.Region
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import java.io.InputStream
import java.util.Base64
import java.util.function.Supplier

/***
 * Class that holds values needed to authenticate against OCI's object storage
 */
data class OCIConfiguration(
    val tenancyOCID: String,
    val userOCID: String,
    val fingerPrint: String,
    val privateKey: String,
    val nameSpace: String,
    val bucketName: String,
    val region: Region = Region.US_PHOENIX_1
) {
    private val privateKeySupplier: Supplier<InputStream> = Supplier<InputStream> { Base64.getDecoder().decode(privateKey).inputStream() }
    private val authProvider by lazy {
        SimpleAuthenticationDetailsProvider.builder()
            .tenantId(tenancyOCID)
            .userId(userOCID)
            .fingerprint(fingerPrint)
            .region(region)
            .privateKeySupplier(privateKeySupplier)
            .build()
    }

    fun getAuthentication(): AuthenticationDetailsProvider {
        return authProvider
    }
}
