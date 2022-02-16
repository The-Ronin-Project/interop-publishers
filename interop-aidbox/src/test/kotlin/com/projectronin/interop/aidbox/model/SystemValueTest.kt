package com.projectronin.interop.aidbox.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SystemValueTest {
    @Test
    fun `getQueryString works`() {
        val systemValue = SystemValue(system = "system", value = "value")
        assertEquals("system|value", systemValue.queryString)
    }
}
