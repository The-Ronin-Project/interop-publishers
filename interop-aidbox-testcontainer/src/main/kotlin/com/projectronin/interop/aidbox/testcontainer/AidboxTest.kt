package com.projectronin.interop.aidbox.testcontainer

import com.projectronin.interop.aidbox.testcontainer.extension.AidboxExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Annotation indicating the annotated class supports Aidbox and injecting [data][AidboxData].
 *
 * This Annotation requires the annotated class, its companion object or any subclass (or their companion objects) to contain
 * an [AidboxContainer][com.projectronin.interop.aidbox.testcontainer.container.AidboxContainer] property which will be
 * used to inject the requested data. Note that this AidboxContainer must also be marked as a [Container][org.testcontainers.junit.jupiter.Container]
 * to ensure it is deployed properly.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Testcontainers
@ExtendWith(AidboxExtension::class)
annotation class AidboxTest
