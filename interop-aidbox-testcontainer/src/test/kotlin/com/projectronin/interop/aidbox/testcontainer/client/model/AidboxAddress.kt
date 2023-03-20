package com.projectronin.interop.aidbox.testcontainer.client.model

/**
 * Describes the Aidbox Address type. This is not intended to be comprehensive and is only focused on current needs.
 */
data class AidboxAddress(
    val use: String? = null,
    val line: List<String>? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null
)
