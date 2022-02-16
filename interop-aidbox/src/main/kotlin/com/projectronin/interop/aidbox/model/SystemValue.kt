package com.projectronin.interop.aidbox.model

data class SystemValue(val value: String, val system: String) {
    val queryString = "$system|$value"
}
