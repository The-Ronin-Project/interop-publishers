package com.projectronin.interop.aidbox.utils

import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.fhir.r4.datatype.primitive.Code

fun <T> CodedEnum<T>.asCode() = Code(code)
