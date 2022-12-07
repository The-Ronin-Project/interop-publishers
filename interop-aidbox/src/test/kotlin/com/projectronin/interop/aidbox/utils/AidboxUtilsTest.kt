package com.projectronin.interop.aidbox.utils

import com.projectronin.interop.aidbox.exception.InvalidTenantAccessException
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AidboxUtilsTest {
    @BeforeEach
    fun setup() {
        mockkStatic("io.ktor.client.statement.HttpResponseKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("io.ktor.client.statement.HttpResponseKt")
    }

    @Test
    fun `respondToGraphQLException handles unsupported exception`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                respondToGraphQLException<String>(IllegalStateException())
            }
        }
    }

    @Test
    fun `ensure validateTenantIdentifier continues when tenant identifier matches`() {
        assertDoesNotThrow {
            validateTenantIdentifier(
                "tenant1",
                listOf(
                    Identifier(system = CodeSystem.RONIN_TENANT.uri, value = "tenant1".asFHIR()),
                    Identifier(system = Uri("otherIdentifierSystem"), value = "123".asFHIR())
                ),
                "Tenant did not match"
            )
        }
    }

    @Test
    fun `ensure validateTenantIdentifier throw exception with different tenant value`() {
        assertThrows<InvalidTenantAccessException> {
            validateTenantIdentifier(
                "tenant1",
                listOf(Identifier(system = CodeSystem.RONIN_TENANT.uri, value = "tenant2".asFHIR())),
                "Tenant did not match"
            )
        }
    }

    @Test
    fun `ensure validateTenantIdentifier throw exception when no value`() {
        assertThrows<InvalidTenantAccessException> {
            validateTenantIdentifier(
                "tenant1",
                listOf(Identifier(system = CodeSystem.RONIN_TENANT.uri, value = null)),
                "Tenant did not match"
            )
        }
    }

    @Test
    fun `ensure validateTenantIdentifier throw exception when value with only extension`() {
        assertThrows<InvalidTenantAccessException> {
            validateTenantIdentifier(
                "tenant1",
                listOf(
                    Identifier(
                        system = CodeSystem.RONIN_TENANT.uri,
                        value = FHIRString(
                            value = null,
                            extension = listOf(
                                Extension(id = FHIRString("1234"))
                            )
                        )
                    )
                ),
                "Tenant did not match"
            )
        }
    }

    @Test
    fun `ensure validateTenantIdentifier throw exception with no tenant identifier systems`() {
        assertThrows<InvalidTenantAccessException> {
            validateTenantIdentifier(
                "tenant1",
                listOf(Identifier(system = Uri("otherUri"), value = "tenant1".asFHIR())),
                "Tenant did not match"
            )
        }
    }

    @Test
    fun `ensure validateTenantIdentifier throw exception with no systems`() {
        assertThrows<InvalidTenantAccessException> {
            validateTenantIdentifier(
                "tenant1",
                listOf(Identifier(system = null, value = "tenant1".asFHIR())),
                "Tenant did not match"
            )
        }
    }

    @Test
    fun `ensure validateTenantIdentifier throw exception when no identifiers in list`() {
        assertThrows<InvalidTenantAccessException> {
            validateTenantIdentifier(
                "tenant1",
                listOf(),
                "Tenant did not match"
            )
        }
    }
}
