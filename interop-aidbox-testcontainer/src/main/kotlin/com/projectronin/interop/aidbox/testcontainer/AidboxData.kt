package com.projectronin.interop.aidbox.testcontainer

/**
 * Annotation supporting injecting data into an [AidboxContainer][com.projectronin.interop.aidbox.testcontainer.container.AidboxContainer]
 * instance of Aidbox.
 *
 * The supplied files must be YAML, though no distinction is made about the file suffix used. Each file can represent one
 * or more resources. The resource type should be provided for all resources to ensure it can be properly inserted. Aidbox
 * will auto-generate an ID if one is not provided, but no methods are provided for accessing the Aidbox-generated ID.
 *
 * For files with one resource, they can either be represented as a single item or as a single item in a list. For files
 * with multiple resources, they should all be represented as items within a list.
 *
 * AidboxData can be provided at either the class or method level. Files declared at the class level will provide data
 * that can be used throughout all tests within the class, while method level files will only provide their data to the
 * annotated method.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AidboxData(vararg val yamlFiles: String)
