package com.projectronin.interop.aidbox.testcontainer.client.model

/**
 * Describes the Aidbox Patient type. This is not intended to be comprehensive and is only focused on current needs.
 */
data class AidboxPatient(
    val id: String? = null,
    val identifier: List<AidboxIdentifier>? = null,
    val name: List<AidboxHumanName>? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val telecom: List<AidboxContactPoint>? = null,
    val address: List<AidboxAddress>? = null,
)
