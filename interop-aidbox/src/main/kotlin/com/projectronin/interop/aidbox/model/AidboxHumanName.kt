package com.projectronin.interop.aidbox.model

/**
 * Describes the Aidbox HumanName type. This is not intended to be comprehensive and is only focused on current needs.
 */
data class AidboxHumanName(
    val use: String? = null,
    val family: String? = null,
    val given: List<String>? = null
)
