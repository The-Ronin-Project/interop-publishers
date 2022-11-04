package com.projectronin.interop.kafka.event

data class PatientEvent(
    val tenantId: String,
    val patientJson: String
)
