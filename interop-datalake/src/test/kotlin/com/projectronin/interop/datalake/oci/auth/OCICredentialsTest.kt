package com.projectronin.interop.datalake.oci.auth

import com.oracle.bmc.Region
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider.SimpleAuthenticationDetailsProviderBuilder
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.Base64

class OCICredentialsTest {
    @Test
    fun `can create`() {
        val credentials = OCICredentials(
            tenantId = "ocid1.tenancy.oc1",
            userId = "ocid1.user.oc1.",
            fingerPrint = "a1:",
            region = Region.US_PHOENIX_1,
            privateKey = "-----BEGIN PRIVATE KEY-----\n" +
                "-----END PRIVATE KEY-----"
        )
        assertEquals("ocid1.tenancy.oc1", credentials.tenantId)
        assertEquals("ocid1.user.oc1.", credentials.userId)
        assertEquals("a1:", credentials.fingerPrint)
        assertEquals(Region.US_PHOENIX_1, credentials.region)
        assertEquals("-----BEGIN PRIVATE KEY-----\n-----END PRIVATE KEY-----", credentials.privateKey)
    }

    @Test
    fun `getAuthentication - works`() {
        mockkConstructor(SimpleAuthenticationDetailsProviderBuilder::class)
        val privateString = "-----BEGIN PRIVATE KEY-----\n" +
            "-----END PRIVATE KEY-----"
        val credentials = OCICredentials(
            tenantId = "ocid1.tenancy.oc1",
            userId = "ocid1.user.oc1.",
            fingerPrint = "a1:",
            region = Region.US_PHOENIX_1,
            privateKey = Base64.getEncoder().encodeToString(privateString.toByteArray())
        )
        val auth = credentials.getAuthentication()

        assertNotNull(auth)
        assertNotNull(auth.privateKey)
        unmockkAll()
    }
}
