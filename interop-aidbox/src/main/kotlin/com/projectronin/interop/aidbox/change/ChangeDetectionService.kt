package com.projectronin.interop.aidbox.change

import com.projectronin.interop.aidbox.client.AidboxClient
import com.projectronin.interop.common.reflect.copy
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Resource
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.util.Collections
import java.util.WeakHashMap

/**
 * The ChangeDetectionService is used to determine if resources should be considered changed since their last update into Aidbox.
 */
@Component
class ChangeDetectionService(private val aidboxClient: AidboxClient) {
    private val resourceHashesByType: MutableMap<String, MutableMap<String, Int>> =
        Collections.synchronizedMap(WeakHashMap())

    /**
     * Filters the [resources] based on whether or not they should be considered changed. Only resources that should be
     * considered changed, including any new or unknown resources, will be included in the response.
     */
    fun filterChangedResources(resources: List<Resource<*>>): List<Resource<*>> =
        resources.filter { isChanged(it) }

    private fun isChanged(resource: Resource<*>): Boolean {
        val resourceType = resource.resourceType
        val resourceId = resource.id!!

        val resourceHash = resource.hashCode()
        val currentHash = getStoredHash(resourceType, resourceId)

        // This check also implicitly handles null current hashes.
        if (resourceHash != currentHash) {
            storeNewHash(resourceType, resourceId, resourceHash)
            return true
        }

        val storedResource = getStoredResource(resourceType, resourceId)
        val normalizedStored = normalizeResource(storedResource)
        val normalizedNew = normalizeResource(resource)
        return normalizedNew != normalizedStored
    }

    private fun getStoredHash(resourceType: String, fhirId: Id): Int? =
        getResourceHashesForType(resourceType)[fhirId.value!!]

    private fun storeNewHash(resourceType: String, fhirId: Id, hashCode: Int) {
        getResourceHashesForType(resourceType)[fhirId.value!!] = hashCode
    }

    private fun getResourceHashesForType(resourceType: String): MutableMap<String, Int> =
        resourceHashesByType.computeIfAbsent(resourceType) { Collections.synchronizedMap(WeakHashMap()) }

    private fun getStoredResource(resourceType: String, fhirId: Id): Resource<*> = runBlocking {
        aidboxClient.getResource(resourceType, fhirId.value!!).body()
    }

    private fun normalizeResource(resource: Resource<*>): Resource<*> =
        copy(resource, mapOf("meta" to null))
}
