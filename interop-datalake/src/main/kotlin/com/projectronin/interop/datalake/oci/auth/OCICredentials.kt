package com.projectronin.interop.datalake.oci.auth

import com.oracle.bmc.Region
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import org.springframework.beans.factory.annotation.Value
import java.io.InputStream
import java.util.function.Supplier

/***
 * Class that holds values needed to authenticate against OCI's object storage
 */
data class OCICredentials(
    @Value("\${datalake.oci.tenant.id}")
    val tenantId: String,
    @Value("\${datalake.oci.user.id}")
    val userId: String,
    @Value("\${datalake.oci.fingerPrint}")
    val fingerPrint: String,
    @Value("\${datalake.oci.privateKey}")
    val privateKey: String,
    @Value("\${datalake.oci.region}")
    val region: Region
) {
    private val privateKeySupplier: Supplier<InputStream> = Supplier<InputStream> { privateKey.byteInputStream() }
    private val authProvider by lazy {
        SimpleAuthenticationDetailsProvider.builder()
            .tenantId(tenantId)
            .userId(userId)
            .fingerprint(fingerPrint)
            .region(region)
            .privateKeySupplier(privateKeySupplier)
            .build()
    }

    fun getAuthentication(): AuthenticationDetailsProvider {
        return authProvider
    }
}
