package com.projectronin.interop.aidbox.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.projectronin.interop.fhir.r4.datatype.Identifier

data class AidboxIdentifiers(
    @JsonProperty("id") val udpId: String,
    @JsonProperty("identifier") val identifiers: List<Identifier>
)
