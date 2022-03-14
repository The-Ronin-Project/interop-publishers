package com.projectronin.interop.aidbox.model.fhir.resource

import com.projectronin.interop.aidbox.model.fhir.datatype.BundleEntry
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Signature
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.base.BaseBundle
import com.projectronin.interop.fhir.r4.valueset.BundleType

/**
 * A container for a collection of FHIR resources, for use with Aidbox publish.
 *
 * See [FHIR Spec](http://www.hl7.org/fhir/bundle.html)
 */
data class Bundle(
    override val id: Id?,
    override val meta: Meta? = null,
    override val implicitRules: Uri? = null,
    override val language: Code? = null,
    override val identifier: Identifier? = null,
    override val type: BundleType,
    override val timestamp: Instant? = null,
    override val total: UnsignedInt? = null,
    override val link: List<BundleLink> = listOf(),
    override val entry: List<BundleEntry> = listOf(),
    override val signature: Signature? = null
) : Resource, BaseBundle<BundleEntry>() {
    init {
        validate()
    }
}
