package com.projectronin.interop.datalake.azure.auth

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class AzureAuthenticationBrokerTest {
    private val mockAuthService = mockk<AzureAuthenticationService>()
    private lateinit var broker: AzureAuthenticationBroker

    private val cachedAuthorizationProperty =
        AzureAuthenticationBroker::class.memberProperties.first { it.name == "cachedAuthentication" } as KMutableProperty<*>

    init {
        cachedAuthorizationProperty.isAccessible = true
    }

    @BeforeEach
    fun initTest() {
        broker = AzureAuthenticationBroker(mockAuthService)
    }
    @Test
    fun `cached works for valid token`() {
        val cachedAuthentication = mockk<AzureAuthentication> {
            every { accessToken } returns "123"
            every { expiresAt } returns Instant.now().plusSeconds(36000)
        }
        setCachedAuthentication(cachedAuthentication)
        val auth = broker.getAuthentication()
        assertEquals("123", auth.accessToken)
    }

    @Test
    fun `cached falls back for expired token`() {
        val cachedAuthentication = mockk<AzureAuthentication> {
            every { expiresAt } returns Instant.now().minusSeconds(36000)
        }
        setCachedAuthentication(cachedAuthentication)
        every { mockAuthService.getAuthentication() } returns mockk {
            every { accessToken } returns "retrieved"
        }
        val auth = broker.getAuthentication()
        assertEquals("retrieved", auth.accessToken)
    }

    @Test
    fun `works when no cached value`() {
        every { mockAuthService.getAuthentication() } returns mockk {
            every { accessToken } returns "retrieved"
        }
        val auth = broker.getAuthentication()
        assertEquals("retrieved", auth.accessToken)
    }

    private fun setCachedAuthentication(authentication: AzureAuthentication?) =
        cachedAuthorizationProperty.setter.call(broker, authentication)
}
