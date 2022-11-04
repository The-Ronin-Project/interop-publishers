package com.projectronin.interop.kafka.event

data class AppointmentEvent(
    val tenantId: String,
    val appointmentJson: String
)
