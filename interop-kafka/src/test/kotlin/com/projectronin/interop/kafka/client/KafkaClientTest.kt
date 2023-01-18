package com.projectronin.interop.kafka.client

import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.model.KafkaTopic
import com.projectronin.interop.kafka.spring.KafkaCloudConfig
import com.projectronin.interop.kafka.spring.KafkaConfig
import com.projectronin.kafka.RoninProducer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class KafkaClientTest {
    private val cloudConfig = KafkaCloudConfig(
        vendor = "local",
        region = "local"
    )

    private val kafkaConfig = mockk<KafkaConfig> {
        every { cloud } returns cloudConfig
    }

    private val tenantId = "test"

    private val producersProperty =
        KafkaClient::class.memberProperties.find { it.name == "producersByTopicName" }!! as KProperty1<KafkaClient, MutableMap<String, RoninProducer>>

    @BeforeEach
    fun setup() {
        mockkConstructor(RoninProducer::class)
        mockkStatic("com.projectronin.interop.kafka.client.KafkaUtilsKt")

        producersProperty.isAccessible = true
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `creates new producer when none exist for topic`() {
        val topic = mockk<KafkaTopic> {
            every { topicName } returns "test.topic.name"
            every { dataSchema } returns "test.topic.name.schema"
        }
        val event1 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/1"
            every { data } returns "Data 1"
        }

        val producer = mockk<RoninProducer> {
            every { send("event.type", "event/1", "Data 1") } returns mockk {
                every { get() } returns mockk()
            }
        }

        every { createProducer(topic, kafkaConfig) } returns producer

        val client = KafkaClient(kafkaConfig)

        val preCallProducers = producersProperty.get(client)
        assertTrue(preCallProducers.isEmpty())

        val response = client.publishEvents(topic, tenantId, null, listOf(event1))
        assertEquals(1, response.successful.size)
        assertEquals(event1, response.successful[0])

        assertEquals(0, response.failures.size)

        val postCallProducers = producersProperty.get(client)
        assertEquals(1, postCallProducers.size)
        assertEquals(producer, postCallProducers["test.topic.name"])

        verify(exactly = 1) { createProducer(topic, kafkaConfig) }
    }

    @Test
    fun `reuses producer when one exists for topic`() {
        val topic = mockk<KafkaTopic> {
            every { topicName } returns "test.topic.name"
            every { dataSchema } returns "test.topic.name.schema"
        }
        val event1 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/1"
            every { data } returns "Data 1"
        }

        val producer = mockk<RoninProducer> {
            every { send("event.type", "event/1", "Data 1") } returns mockk {
                every { get() } returns mockk()
            }
        }

        val client = KafkaClient(kafkaConfig)

        val preCallProducers = producersProperty.get(client)
        assertEquals(0, preCallProducers.size)

        preCallProducers["test.topic.name"] = producer

        val response = client.publishEvents(topic, tenantId, null, listOf(event1))
        assertEquals(1, response.successful.size)
        assertEquals(event1, response.successful[0])

        assertEquals(0, response.failures.size)

        val postCallProducers = producersProperty.get(client)
        assertEquals(1, postCallProducers.size)
        assertEquals(producer, postCallProducers["test.topic.name"])

        verify(exactly = 0) { createProducer(topic, kafkaConfig) }
    }

    @Test
    fun `handles send that results in an exception`() {
        val topic = mockk<KafkaTopic> {
            every { topicName } returns "test.topic.name"
            every { dataSchema } returns "test.topic.name.schema"
        }
        val event1 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/1"
            every { data } returns "Data 1"
        }

        val producer = mockk<RoninProducer> {
            every { send("event.type", "event/1", "Data 1") } returns mockk {
                every { get() } throws IllegalStateException("exception")
            }
        }

        every { createProducer(topic, kafkaConfig) } returns producer

        val client = KafkaClient(kafkaConfig)
        val response = client.publishEvents(topic, tenantId, null, listOf(event1))
        assertEquals(0, response.successful.size)

        assertEquals(1, response.failures.size)
        assertEquals(event1, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals("exception", response.failures[0].error.message)
    }

    @Test
    fun `handles send that results in success`() {
        val topic = mockk<KafkaTopic> {
            every { topicName } returns "test.topic.name"
            every { dataSchema } returns "test.topic.name.schema"
        }
        val event1 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/1"
            every { data } returns "Data 1"
        }

        val producer = mockk<RoninProducer> {
            every { send("event.type", "event/1", "Data 1") } returns mockk {
                every { get() } returns mockk()
            }
        }

        every { createProducer(topic, kafkaConfig) } returns producer

        val client = KafkaClient(kafkaConfig)
        val response = client.publishEvents(topic, tenantId, null, listOf(event1))
        assertEquals(1, response.successful.size)
        assertEquals(event1, response.successful[0])

        assertEquals(0, response.failures.size)
    }

    @Test
    fun `handles non-null trigger`() {
        val topic = mockk<KafkaTopic> {
            every { topicName } returns "test.topic.name-nightly"
            every { dataSchema } returns "test.topic.name.schema"
        }
        val event1 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/1"
            every { data } returns "Data 1"
        }

        val producer = mockk<RoninProducer> {
            every { send("event.type", "event/1", "Data 1") } returns mockk {
                every { get() } returns mockk()
            }
        }

        every { createProducer(topic, kafkaConfig) } returns producer

        val client = KafkaClient(kafkaConfig)
        val response = client.publishEvents(topic, tenantId, DataTrigger.NIGHTLY, listOf(event1))
        assertEquals(1, response.successful.size)
        assertEquals(event1, response.successful[0])

        assertEquals(0, response.failures.size)
    }

    @Test
    fun `handles some results are failure`() {
        val topic = mockk<KafkaTopic> {
            every { topicName } returns "test.topic.name"
            every { dataSchema } returns "test.topic.name.schema"
        }
        val event1 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/1"
            every { data } returns "Data 1"
        }
        val event2 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/2"
            every { data } returns "Data 2"
        }
        val event3 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/3"
            every { data } returns "Data 3"
        }

        val producer = mockk<RoninProducer> {
            every { send("event.type", "event/1", "Data 1") } returns mockk {
                every { get() } returns mockk()
            }
            every { send("event.type", "event/2", "Data 2") } returns mockk {
                every { get() } throws IllegalStateException("exception")
            }
            every { send("event.type", "event/3", "Data 3") } returns mockk {
                every { get() } returns mockk()
            }
        }

        every { createProducer(topic, kafkaConfig) } returns producer

        val client = KafkaClient(kafkaConfig)
        val response = client.publishEvents(topic, tenantId, null, listOf(event1, event2, event3))
        assertEquals(2, response.successful.size)
        assertEquals(event1, response.successful[0])
        assertEquals(event3, response.successful[1])

        assertEquals(1, response.failures.size)
        assertEquals(event2, response.failures[0].data)
        assertInstanceOf(IllegalStateException::class.java, response.failures[0].error)
        assertEquals("exception", response.failures[0].error.message)
    }

    @Test
    fun `handles all results are success`() {
        val topic = mockk<KafkaTopic> {
            every { topicName } returns "test.topic.name"
            every { dataSchema } returns "test.topic.name.schema"
        }
        val event1 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/1"
            every { data } returns "Data 1"
        }
        val event2 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/2"
            every { data } returns "Data 2"
        }
        val event3 = mockk<KafkaEvent<String>> {
            every { type } returns "event.type"
            every { subject } returns "event/3"
            every { data } returns "Data 3"
        }

        val producer = mockk<RoninProducer> {
            every { send("event.type", "event/1", "Data 1") } returns mockk {
                every { get() } returns mockk()
            }
            every { send("event.type", "event/2", "Data 2") } returns mockk {
                every { get() } returns mockk()
            }
            every { send("event.type", "event/3", "Data 3") } returns mockk {
                every { get() } returns mockk()
            }
        }

        every { createProducer(topic, kafkaConfig) } returns producer

        val client = KafkaClient(kafkaConfig)
        val response = client.publishEvents(topic, tenantId, null, listOf(event1, event2, event3))
        assertEquals(3, response.successful.size)
        assertEquals(event1, response.successful[0])
        assertEquals(event2, response.successful[1])
        assertEquals(event3, response.successful[2])

        assertEquals(0, response.failures.size)
    }
}
