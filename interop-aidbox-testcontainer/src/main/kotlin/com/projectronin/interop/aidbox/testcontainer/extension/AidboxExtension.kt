package com.projectronin.interop.aidbox.testcontainer.extension

import com.projectronin.interop.aidbox.testcontainer.AidboxData
import com.projectronin.interop.aidbox.testcontainer.container.AidboxContainer
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.util.Optional
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 * Extension for injecting [AidboxData] into an [AidboxContainer].
 */
class AidboxExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private val logger = KotlinLogging.logger { }
    private var aidboxContainer: AidboxContainer? = null
    private val classDataFilesToLoad = mutableListOf<String>()
    private val classResourceIds = mutableMapOf<String, MutableSet<String>>()
    private val methodResourceIds = mutableMapOf<String, MutableSet<String>>()

    override fun beforeAll(context: ExtensionContext) {
        val testClass = context.testClass.orElse(null) ?: return

        val dataFiles = getDataFiles(testClass.kotlin)
        if (dataFiles.isEmpty()) {
            logger.info { "No Aidbox data files found for class" }
            return
        }

        classDataFilesToLoad.addAll(dataFiles)
    }

    override fun afterAll(context: ExtensionContext) {
        val aidboxContainer = aidboxContainer ?: return
        logger.debug { "Using AidboxContainer $aidboxContainer" }

        purgeData(classResourceIds, aidboxContainer)
    }

    override fun beforeEach(context: ExtensionContext) {
        val testClass = context.testClass.orElse(null) ?: return
        val testInstance = context.testInstances.map { it.findInstance(testClass).orElse(null) }.orElse(null) ?: return

        val aidboxContainer = findAidboxContainer(testClass.kotlin, testInstance)!!
        logger.debug { "Using AidboxContainer $aidboxContainer" }

        // If we have any class data files that haven't been processed (ie, it's the first method for a class annotated test), then process them.
        if (classDataFilesToLoad.isNotEmpty()) {
            persistData(classDataFilesToLoad, aidboxContainer, classResourceIds)
            classDataFilesToLoad.clear()
        }

        val dataFiles = getDataFiles(context.testMethod)
        if (dataFiles.isEmpty()) {
            logger.info { "No Aidbox data files found for method" }
            return
        }

        persistData(dataFiles, aidboxContainer, methodResourceIds)
    }

    override fun afterEach(context: ExtensionContext) {
        val testClass = context.testClass.orElse(null) ?: return
        val testInstance = context.testInstances.map { it.findInstance(testClass).orElse(null) }.orElse(null) ?: return

        val aidboxContainer = findAidboxContainer(testClass.kotlin, testInstance)!!
        logger.debug { "Using AidboxContainer $aidboxContainer" }

        purgeData(methodResourceIds, aidboxContainer)
    }

    /**
     * Finds the [AidboxContainer] for the supplied [testClass] and [testInstance].
     */
    private fun findAidboxContainer(testClass: KClass<*>, testInstance: Any): AidboxContainer? {
        if (aidboxContainer == null) {
            aidboxContainer = getAidboxContainer(testClass, testInstance)
                ?: getAidboxContainer(testClass.companionObject, testClass.companionObjectInstance)

            if (aidboxContainer == null) {
                for (superclass in testClass.superclasses) {
                    aidboxContainer = findAidboxContainer(superclass, testInstance)
                    if (aidboxContainer != null) {
                        break
                    }
                }
            }

            aidboxContainer ?: throw IllegalStateException("No AidboxContainer present on test")
        }

        logger.debug { "Using AidboxContainer $aidboxContainer" }
        return aidboxContainer
    }

    /**
     * Attempts to retrieve the [AidboxContainer] from a member property for a supplied [testClass] and [instance]
     */
    private fun getAidboxContainer(testClass: KClass<*>?, instance: Any?): AidboxContainer? {
        if (testClass == null || instance == null) {
            return null
        }

        val containerProperty =
            testClass.memberProperties.find { it.returnType.jvmErasure.isSubclassOf(AidboxContainer::class) } as KProperty1<Any, AidboxContainer>?
        return containerProperty?.let { getContainer(it, instance) }
    }

    /**
     * Gets the container from the [property] for the [instance], handling any accessibility issues.
     */
    private fun <T> getContainer(property: KProperty1<Any, T>, instance: Any): T {
        val accessible = property.getter.isAccessible
        property.getter.isAccessible = true
        val container = property.get(instance)
        property.getter.isAccessible = accessible
        return container
    }

    /**
     * Retrieves the data files from an [AidboxData] present on the [clazz], or an empty List if none are found.
     */
    private fun getDataFiles(clazz: KClass<*>): List<String> {
        val dataAnnotation = clazz.findAnnotation<AidboxData>()
        return dataAnnotation?.yamlFiles?.toList() ?: listOf()
    }

    /**
     * Retrieves the data files from an [AidboxData] present on the [method], or an empty List if none are found.
     */
    private fun getDataFiles(method: Optional<Method>): List<String> {
        val dataAnnotation = method.orElse(null)?.getAnnotation(AidboxData::class.java)
        return dataAnnotation?.yamlFiles?.toList() ?: listOf()
    }

    /**
     * Reads the provided set of [files] into a single YAML string.
     */
    private fun readFiles(files: List<String>): String {
        val builder = StringBuilder()
        for (file in files) {
            val normalizedFile = if (file.startsWith("/")) file else "/$file"
            val text = this.javaClass.getResource(normalizedFile).readText(Charset.defaultCharset()).trim()
            if (text.startsWith("-")) {
                builder.append(text)
                builder.appendLine()
            } else {
                logger.debug { "Treating $file as a single resource" }
                val lines = text.split("\n")
                builder.append("- ${lines.joinToString("\n  ")}")
                builder.appendLine()
            }
        }
        return builder.toString()
    }

    /**
     * Persists the data from the [files] to the [aidboxContainer] instance. [resourceIds] will be updated for any inserted resources.
     */
    private fun persistData(
        files: List<String>,
        aidboxContainer: AidboxContainer,
        resourceIds: MutableMap<String, MutableSet<String>>
    ) {
        val data = readFiles(files)
        runBlocking {
            val response = aidboxContainer.ktorClient.put("${aidboxContainer.baseUrl()}/") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${aidboxContainer.accessToken()}")
                }
                accept(ContentType.Application.Json)
                setBody(TextContent(data, contentType = ContentType("text", "yaml")))
            }

            if (!response.status.isSuccess()) {
                throw IllegalStateException("Error while priming test data: ${response.bodyAsText()}")
            }

            val items = response.body<List<UpsertedItem>>()
            items.forEach { resourceIds.computeIfAbsent(it.resourceType) { mutableSetOf() }.add(it.id) }
        }
    }

    /**
     * Deletes the [resourceIds] from the [aidboxContainer], resetting the [resourceIds] when complete.
     */
    private fun purgeData(resourceIds: MutableMap<String, out Set<String>>, aidboxContainer: AidboxContainer) {
        runBlocking {
            val aidboxUrl = aidboxContainer.baseUrl()
            for ((resource, ids) in resourceIds) {
                for (id in ids) {
                    val response = aidboxContainer.ktorClient.delete("$aidboxUrl/$resource/$id") {
                        headers {
                            append(HttpHeaders.Authorization, "Bearer ${aidboxContainer.accessToken()}")
                        }
                    }

                    if (!response.status.isSuccess()) {
                        throw IllegalStateException("Error while purging test data: ${response.bodyAsText()}")
                    }
                }
            }
        }

        resourceIds.clear()
    }

    data class UpsertedItem(val resourceType: String, val id: String)
}
