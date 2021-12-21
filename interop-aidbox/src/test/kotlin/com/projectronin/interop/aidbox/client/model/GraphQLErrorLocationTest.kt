package com.projectronin.interop.aidbox.client.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraphQLErrorLocationTest {
    @Test
    fun `check getters`() {
        val location = GraphQLErrorLocation(5, 7)
        assertEquals(5, location.line)
        assertEquals(7, location.column)
    }
}
