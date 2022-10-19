package com.projectronin.interop.datalake.oci.auth

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.Base64

class OCIConfigurationTest {

    @Test
    fun `getAuthentication - works`() {
        mockkConstructor(SimpleAuthenticationDetailsProviderBuilder::class)
        val privateString = "-----BEGIN PRIVATE KEY-----\n" +
            "-----END PRIVATE KEY-----"
        val credentials = OCIConfiguration(
            tenancyOCID = "ocid1.tenancy.oc1",
            userOCID = "ocid1.user.oc1.",
            fingerPrint = "a1:",
            privateKey = Base64.getEncoder().encodeToString(privateString.toByteArray()),
            nameSpace = "Namespace",
            bucketName = "Bucket"
        )
        val auth = credentials.getAuthentication()

        assertNotNull(auth)
        assertNotNull(auth.privateKey)
        unmockkAll()
    }
}
