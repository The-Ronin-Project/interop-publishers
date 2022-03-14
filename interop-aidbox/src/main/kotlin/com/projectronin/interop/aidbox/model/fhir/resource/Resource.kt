package com.projectronin.interop.aidbox.model.fhir.resource

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.projectronin.interop.fhir.FHIRResource
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri

/**
 * A resource is an entity that:
 * - has a known identity (a URL) by which it can be addressed
 * - identifies itself as one of the types of resource defined in this specification
 * - contains a set of structured data items as described by the definition of the resource type
 * - has an identified version that changes if the contents of the resource change
 *
 * See [FHIR Spec](https://www.hl7.org/fhir/resource.html)
 */
@JsonPropertyOrder("resourceType")
interface Resource : FHIRResource {
    override val resourceType: String
    override val id: Id?
    val meta: Meta?
    val implicitRules: Uri?
    val language: Code?
}
