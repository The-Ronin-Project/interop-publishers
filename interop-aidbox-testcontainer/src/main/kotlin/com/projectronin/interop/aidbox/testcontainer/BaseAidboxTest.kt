package com.projectronin.interop.aidbox.testcontainer

import com.projectronin.interop.aidbox.testcontainer.container.AidboxContainer
import com.projectronin.interop.aidbox.testcontainer.container.AidboxDatabaseContainer
import org.testcontainers.junit.jupiter.Container

/**
 * Base test for Aidbox tests. Any extending classes may provide their own [AidboxData] at the class or method level for
 * inclusion in their specific tests.
 */
@AidboxTest
abstract class BaseAidboxTest {
    companion object {
        @Container
        val aidboxDatabaseContainer = AidboxDatabaseContainer()

        @Container
        val aidbox = AidboxContainer(aidboxDatabaseContainer)
    }
}
