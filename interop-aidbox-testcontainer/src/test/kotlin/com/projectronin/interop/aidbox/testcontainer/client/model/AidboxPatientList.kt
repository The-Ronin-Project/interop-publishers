package com.projectronin.interop.aidbox.testcontainer.client.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Describes the Aidbox PatientList resource.
 */
data class AidboxPatientList(
    @JsonProperty("PatientList")
    val patientList: List<AidboxPatient> = listOf(),
)
