package com.projectronin.interop.kafka.spring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TopicConfigTest {

    @Test
    fun `get topics`() {
        assertEquals(1, LoadSpringConfig().loadTopics().size)
        assertEquals(2, PublishSpringConfig().publishTopics().size)
    }
}
