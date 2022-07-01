package com.projectronin.interop.aidbox.model.fhir.datatype

import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.BundleLink
import com.projectronin.interop.fhir.r4.datatype.BundleRequest
import com.projectronin.interop.fhir.r4.datatype.BundleResponse
import com.projectronin.interop.fhir.r4.datatype.BundleSearch
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.LocationHoursOfOperation
import com.projectronin.interop.fhir.r4.datatype.LocationPosition
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.ContainedResource
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.ronin.resource.OncologyPatient
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.HttpVerb
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.SearchEntryMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BundleEntryTest {
    private val oncologyPatient = OncologyPatient(
        identifier = listOf(
            Identifier(
                system = CodeSystem.RONIN_TENANT.uri,
                type = CodeableConcepts.RONIN_TENANT,
                value = "tenantId"
            ),
            Identifier(
                system = CodeSystem.MRN.uri,
                type = CodeableConcepts.MRN,
                value = "MRN"
            ),
            Identifier(
                system = CodeSystem.FHIR_STU3_ID.uri,
                type = CodeableConcepts.FHIR_STU3_ID,
                value = "fhirId"
            )
        ),
        name = listOf(HumanName(family = "Doe")),
        telecom = listOf(
            ContactPoint(
                system = ContactPointSystem.PHONE,
                value = "8675309",
                use = ContactPointUse.MOBILE
            )
        ),
        gender = AdministrativeGender.FEMALE,
        birthDate = Date("1975-07-05"),
        address = listOf(Address(country = "USA")),
        maritalStatus = CodeableConcept(text = "M")
    )
    val type = listOf(
        CodeableConcept(
            text = "Diagnostic",
            coding = listOf(
                Coding(
                    code = Code("DX"),
                    system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType")
                )
            )
        )
    )
    val physicalType = CodeableConcept(
        text = "Room",
        coding = listOf(
            Coding(
                code = Code("ro"),
                system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type")
            )
        )
    )
    val location = Location(
        id = Id("12345"),
        meta = Meta(
            profile = listOf(Canonical("https://www.hl7.org/fhir/location")),
        ),
        implicitRules = Uri("implicit-rules"),
        language = Code("en-US"),
        text = Narrative(
            status = NarrativeStatus.GENERATED,
            div = "div"
        ),
        contained = listOf(ContainedResource("""{"resourceType":"Banana","field":"24680"}""")),
        extension = listOf(
            Extension(
                url = Uri("http://localhost/extension"),
                value = DynamicValue(DynamicValueType.STRING, "Value")
            )
        ),
        modifierExtension = listOf(
            Extension(
                url = Uri("http://localhost/modifier-extension"),
                value = DynamicValue(DynamicValueType.STRING, "Value")
            )
        ),
        identifier = listOf(Identifier(value = "id")),
        mode = LocationMode.INSTANCE,
        status = LocationStatus.ACTIVE,
        name = "My Office",
        alias = listOf("Guest Room"),
        description = "Sun Room",
        type = type,
        telecom = listOf(ContactPoint(system = ContactPointSystem.PHONE, value = "8675309")),
        address = Address(country = "USA"),
        physicalType = physicalType,
        position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132)),
        hoursOfOperation = listOf(
            LocationHoursOfOperation(
                daysOfWeek = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                allDay = true
            )
        ),
        availabilityExceptions = "Call for details",
        endpoint = listOf(Reference(reference = "Endpoint/4321"))
    )

    @Test
    fun `can serialize JSON (R4 resource)`() {
        val bundleEntry = BundleEntry(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            link = listOf(BundleLink(relation = "next", url = Uri("http://www.example.com/next"))),
            resource = location,
            search = BundleSearch(mode = SearchEntryMode.INCLUDE),
            request = BundleRequest(method = HttpVerb.GET, url = Uri("http://www.example.com/get")),
            response = BundleResponse(status = "Ok")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleEntry)

        val expectedJson = """
            {
              "id" : "12345",
              "extension" : [ {
                "url" : "http://localhost/extension",
                "valueString" : "Value"
              } ],
              "modifierExtension" : [ {
                "url" : "http://localhost/modifier-extension",
                "valueString" : "Value"
              } ],
              "link" : [ {
                "relation" : "next",
                "url" : "http://www.example.com/next"
              } ],
              "resource" : {
                "resourceType" : "Location",
                "id" : "12345",
                "meta" : {
                  "profile" : [ "https://www.hl7.org/fhir/location" ]
                },
                "implicitRules" : "implicit-rules",
                "language" : "en-US",
                "text" : {
                  "status" : "generated",
                  "div" : "div"
                },
                "contained" : [ {"resourceType":"Banana","field":"24680"} ],
                "extension" : [ {
                  "url" : "http://localhost/extension",
                  "valueString" : "Value"
                } ],
                "modifierExtension" : [ {
                  "url" : "http://localhost/modifier-extension",
                  "valueString" : "Value"
                } ],
                "identifier" : [ {
                  "value" : "id"
                } ],
                "status" : "active",
                "name" : "My Office",
                "alias" : [ "Guest Room" ],
                "description" : "Sun Room",
                "mode" : "instance",
                "type" : [ {
                  "coding" : [ {
                    "system" : "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType",
                    "code" : "DX"
                  } ],
                  "text" : "Diagnostic"
                } ],
                "telecom" : [ {
                  "system" : "phone",
                  "value" : "8675309"
                } ],
                "address" : {
                  "country" : "USA"
                },
                "physicalType" : {
                  "coding" : [ {
                    "system" : "http://terminology.hl7.org/CodeSystem/location-physical-type",
                    "code" : "ro"
                  } ],
                  "text" : "Room"
                },
                "position" : {
                  "longitude" : 13.81531,
                  "latitude" : 66.077132
                },
                "hoursOfOperation" : [ {
                  "daysOfWeek" : [ "sat", "sun" ],
                  "allDay" : true
                } ],
                "availabilityExceptions" : "Call for details",
                "endpoint" : [ {
                  "reference" : "Endpoint/4321"
                } ]
              },
              "search" : {
                "mode" : "include"
              },
              "request" : {
                "method" : "GET",
                "url" : "http://www.example.com/get"
              },
              "response" : {
                "status" : "Ok"
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `serialized JSON ignores null and empty fields (R4 resource)`() {
        val bundleEntry = BundleEntry(search = BundleSearch(mode = SearchEntryMode.INCLUDE))

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleEntry)

        val expectedJson = """
            |{
            |  "search" : {
            |    "mode" : "include"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `can serialize JSON (Ronin resource)`() {
        val bundleEntry = BundleEntry(
            id = "12345",
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            link = listOf(BundleLink(relation = "next", url = Uri("http://www.example.com/next"))),
            resource = oncologyPatient,
            search = BundleSearch(mode = SearchEntryMode.INCLUDE),
            request = BundleRequest(method = HttpVerb.GET, url = Uri("http://www.example.com/get")),
            response = BundleResponse(status = "Ok")
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleEntry)

        val expectedJson = """
            {
              "id" : "12345",
              "extension" : [ {
                "url" : "http://localhost/extension",
                "valueString" : "Value"
              } ],
              "modifierExtension" : [ {
                "url" : "http://localhost/modifier-extension",
                "valueString" : "Value"
              } ],
              "link" : [ {
                "relation" : "next",
                "url" : "http://www.example.com/next"
              } ],
              "resource" : {
                "resourceType" : "Patient",
                "identifier" : [ {
                  "type" : {
                    "coding" : [ {
                      "system" : "http://projectronin.com/id/tenantId",
                      "code" : "TID",
                      "display" : "Ronin-specified Tenant Identifier"
                    } ],
                    "text" : "Tenant ID"
                  },
                  "system" : "http://projectronin.com/id/tenantId",
                  "value" : "tenantId"
                }, {
                  "type" : {
                    "coding" : [ {
                      "system" : "http://projectronin.com/id/mrn",
                      "code" : "MR",
                      "display" : "Medical Record Number"
                    } ],
                    "text" : "MRN"
                  },
                  "system" : "http://projectronin.com/id/mrn",
                  "value" : "MRN"
                }, {
                  "type" : {
                    "coding" : [ {
                      "system" : "http://projectronin.com/id/fhir",
                      "code" : "STU3",
                      "display" : "FHIR STU3 ID"
                    } ],
                    "text" : "FHIR STU3"
                  },
                  "system" : "http://projectronin.com/id/fhir",
                  "value" : "fhirId"
                } ],
                "name" : [ {
                  "family" : "Doe"
                } ],
                "telecom" : [ {
                  "system" : "phone",
                  "value" : "8675309",
                  "use" : "mobile"
                } ],
                "gender" : "female",
                "birthDate" : "1975-07-05",
                "address" : [ {
                  "country" : "USA"
                } ],
                "maritalStatus" : {
                  "text" : "M"
                }
              },
              "search" : {
                "mode" : "include"
              },
              "request" : {
                "method" : "GET",
                "url" : "http://www.example.com/get"
              },
              "response" : {
                "status" : "Ok"
              }
            }
        """.trimIndent()
        assertEquals(expectedJson, json)
    }

    @Test
    fun `serialized JSON ignores null and empty fields (Ronin resource)`() {
        val bundleEntry = BundleEntry(search = BundleSearch(mode = SearchEntryMode.INCLUDE))

        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bundleEntry)

        val expectedJson = """
            |{
            |  "search" : {
            |    "mode" : "include"
            |  }
            |}""".trimMargin()
        assertEquals(expectedJson, json)
    }
}
