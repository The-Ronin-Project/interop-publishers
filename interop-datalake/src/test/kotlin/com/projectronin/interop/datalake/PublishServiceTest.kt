package com.projectronin.interop.datalake

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.datalake.azure.client.AzureClient
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Practitioner
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PublishServiceTest {
    private val mockClient = mockk<AzureClient> {}
    private val service = PublishService(mockClient)
    private val tenantId = "mockTenant"

    @Test
    fun `empty FHIR R4 collection is skipped`() {
        assertTrue(service.publishFHIRR4(tenantId, emptyList()))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `real FHIR R4 collection processes`() {
        mockkConstructor(LocalDate::class)
        mockkStatic(LocalDate::class)

        val mockkLocalDate = mockk<LocalDate> {
            every { format(any()) } returns "1990-01-03"
        }
        every { LocalDate.now() } returns mockkLocalDate // LocalDate.of(1990,1,3)

        val location1 = Location(
            id = Id("abc"),
            name = "Location1"
        )
        val location2 = Location(
            id = Id("def"),
            name = "Location2"
        )
        val practitioner = Practitioner(
            id = Id("abc"),
        )
        val filePathString =
            "/fhir-r4/date=1990-01-03/tenant_id=mockTenant/resource_type=__RESOURCETYPE__/__FHIRID__.json"
        val locationFilePathString = filePathString.replace("__RESOURCETYPE__", "Location")
        val practitionerFilePathString = filePathString.replace("__RESOURCETYPE__", "Practitioner")
        val objectMapper = JacksonManager.objectMapper
        justRun {
            mockClient.upload(
                locationFilePathString.replace("__FHIRID__", "abc"),
                objectMapper.writeValueAsString(location1)
            )
        }
        justRun {
            mockClient.upload(
                locationFilePathString.replace("__FHIRID__", "def"),
                objectMapper.writeValueAsString(location2)
            )
        }
        justRun {
            mockClient.upload(
                practitionerFilePathString.replace("__FHIRID__", "abc"),
                objectMapper.writeValueAsString(practitioner)
            )
        }
        assertTrue(service.publishFHIRR4(tenantId, listOf(location1, location2, practitioner)))
        verify(exactly = 3) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `cannot publish a FHIR R4 resource that has no id`() {
        val badResource = Location()
        val exception = assertThrows<IllegalStateException> {
            service.publishFHIRR4(tenantId, listOf(badResource))
        }
        Assertions.assertEquals(
            "Attempted to publish a Location resource without a FHIR ID for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `cannot publish a FHIR R4 resource that has a null id`() {
        val badResource = Location(id = null)
        val exception = assertThrows<IllegalStateException> {
            service.publishFHIRR4(tenantId, listOf(badResource))
        }
        Assertions.assertEquals(
            "Attempted to publish a Location resource without a FHIR ID for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `empty API JSON data is skipped`() {
        assertTrue(service.publishAPIJSON(tenantId, "", "GET", "/fhir/Appointment"))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `empty API JSON method does not process`() {
        val exception = assertThrows<IllegalStateException> {
            service.publishAPIJSON(tenantId, "", "", "/fhir/Appointment")
        }
        Assertions.assertEquals(
            "Attempted to publish JSON data from an API response without identifying the API request for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `empty API JSON url does not process`() {
        val exception = assertThrows<IllegalStateException> {
            service.publishAPIJSON(tenantId, "", "GET", "")
        }
        Assertions.assertEquals(
            "Attempted to publish JSON data from an API response without identifying the API request for tenant $tenantId",
            exception.message
        )
    }

    @Test
    fun `real API JSON processes`() {
        mockkConstructor(LocalDate::class)
        mockkStatic(LocalDate::class)
        val mockkLocalDate = mockk<LocalDate> {
            every { format(DateTimeFormatter.ISO_LOCAL_DATE) } returns "1990-01-03"
        }
        every { LocalDate.now() } returns mockkLocalDate

        mockkConstructor(LocalTime::class)
        mockkStatic(LocalTime::class)
        val mockkLocalTime = mockk<LocalTime> {
            every { format(DateTimeFormatter.ISO_LOCAL_TIME) } returns "06:07:42.999"
        }
        every { LocalTime.now() } returns mockkLocalTime

        val data = """
            {
                "first": "this",
                "second": {
                    "aaa": "that",
                    "bbb": "other"
                }
            }
        """.trimIndent()

        val filePathString =
            "/api-json/schema=GET-customAppointmentByPatient/date=1990-01-03/tenant_id=mockTenant/06-07-42-999.json"
        justRun { mockClient.upload(filePathString, data) }
        assertTrue(service.publishAPIJSON(tenantId, data, "GET", "/custom/AppointmentByPatient"))
        verify(exactly = 1) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `empty list of HL7v2 messages is skipped`() {
        assertTrue(service.publishHL7v2(tenantId, emptyList()))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `empty HL7v2 messages are skipped`() {
        assertTrue(service.publishHL7v2(tenantId, listOf("", "")))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `HL7v2 ADTA01 is skipped as not an EventType Ronin deals with`() {
        val message = """
            MSH|^~\&|HIS|RIH|EKG|EKG|199904140038||ADT^A01||P|2.2
            PID|0001|00009874|00001122|A00977|SMITH^JOHN^M|MOM|19581119|F|NOTREAL^LINDA^M|C|564 SPRING ST^^NEEDHAM^MA^02494^US|0002|(818)565-1551|(425)828-3344|E|S|C|0000444444|252-00-4414||||SA|||SA||||NONE|V1|0001|I|D.ER^50A^M110^01|ER|P00055|11B^M011^02|070615^BATMAN^GEORGE^L|555888^NOTREAL^BOB^K^DR^MD|777889^NOTREAL^SAM^T^DR^MD^PHD|ER|D.WT^1A^M010^01|||ER|AMB|02|070615^NOTREAL^BILL^L|ER|000001916994|D||||||||||||||||GDD|WA|NORM|02|O|02|E.IN^02D^M090^01|E.IN^01D^M080^01|199904072124|199904101200|199904101200||||5555112333|||666097^NOTREAL^MANNY^P
            NK1|0222555|NOTREAL^JAMES^R|FA|STREET^OTHER STREET^CITY^ST^55566|(222)111-3333|(888)999-0000|||||||ORGANIZATION
            PV1|0001|I|D.ER^1F^M950^01|ER|P000998|11B^M011^02|070615^BATMAN^GEORGE^L|555888^OKNEL^BOB^K^DR^MD|777889^NOTREAL^SAM^T^DR^MD^PHD|ER|D.WT^1A^M010^01|||ER|AMB|02|070615^VOICE^BILL^L|ER|000001916994|D||||||||||||||||GDD|WA|NORM|02|O|02|E.IN^02D^M090^01|E.IN^01D^M080^01|199904072124|199904101200|||||5555112333|||666097^DNOTREAL^MANNY^P
            PV2|||0112^TESTING|55555^PATIENT IS NORMAL|NONE|||19990225|19990226|1|1|TESTING|555888^NOTREAL^BOB^K^DR^MD||||||||||PROD^003^099|02|ER||NONE|19990225|19990223|19990316|NONE
            AL1||SEV|001^POLLEN
            GT1||0222PL|NOTREAL^BOB^B||STREET^OTHER STREET^CITY^ST^77787|(444)999-3333|(222)777-5555||||MO|111-33-5555||||NOTREAL GILL N|STREET^OTHER STREET^CITY^ST^99999|(111)222-3333
            IN1||022254P|4558PD|BLUE CROSS|STREET^OTHER STREET^CITY^ST^00990||(333)333-6666||221K|LENIX|||19980515|19990515|||PATIENT01 TEST D||||||||||||||||||02LL|022LP554
        """.trimMargin()

        assertTrue(service.publishHL7v2(tenantId, listOf(message)))
        verify(exactly = 0) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `HL7v2 MDMT02 processes`() {
        mockkConstructor(LocalDate::class)
        mockkStatic(LocalDate::class)
        val mockkLocalDate = mockk<LocalDate> {
            every { format(DateTimeFormatter.ISO_LOCAL_DATE) } returns "1990-01-03"
        }
        every { LocalDate.now() } returns mockkLocalDate

        mockkConstructor(LocalTime::class)
        mockkStatic(LocalTime::class)
        val mockkLocalTime = mockk<LocalTime> {
            every { format(DateTimeFormatter.ISO_LOCAL_TIME) } returns "06:07:42.999"
        }
        every { LocalTime.now() } returns mockkLocalTime

        val messageMDMT02 = """
            MSH|^~\&|Brocade|MDACC|Epic|MDACC|20220412141440||MDM^T02|121157|T|2.7
            EVN|T02|20220412141440
            PID|||2008651^^^EPI^MR||RADONCTEST^CATHERINE||19600812|F||||||
            PV1||O
            TXA||135||20220411000000|00452^JHINGRAN^ANUJA^^^^^^PROVID^^^^PROVID|20220412141440|20220412141440||00452^JHINGRAN^ANUJA^^^^^^PROVID^^^^PROVID|||^^dpg121157_637853696804855070|||||IP||UN|||||||
            OBX|1|TX|||Procedure Date: 04-11-2022
            OBX|2|TX|||
            OBX|3|TX|||Brachytherapy Procedure: #1/3 vaginal cuff brachytherapy
            OBX|4|TX|||
            OBX|5|TX|||The radiation prescription is as follows:
            OBX|6|TX|||
            OBX|7|TX|||
            OBX|8|TX|||Pre-procedure Diagnosis: (C53.9)
            OBX|9|TX|||
            OBX|10|TX|||Post-procedure Diagnosis: (C53.9)
            OBX|11|TX|||
            OBX|12|TX|||Procedure in detail: Catherine Radonctest was brought to the HDR suite and a second
            OBX|13|TX|||timeout was performed. She was positioned supine. A 2 cm dome plus 1 cylinder was
            OBX|14|TX|||placed in the vaginal apex. A fixation device consisting of a perineal bar was used.
            OBX|15|TX|||The positioning of the vaginal dome in the apex was confirmed using kV imaging. 4 dwell
            OBX|16|TX|||positions were utilized at the apex of the dome. Using an iridium-192 source, treatment
            OBX|17|TX|||of 4 cGy was delivered to the vaginal surface over 4 seconds. The dome was then removed
            OBX|18|TX|||and the room was surveyed. The patient tolerated the procedure well without
            OBX|19|TX|||complications.
            OBX|20|TX|||
            OBX|21|TX|||RADIATION ONCOLOGIST'S ADDENDUM: I discussed this case with the fellow and agree with
            OBX|22|TX|||the treatment plan described in the note. I reviewed the pertinent images and treatment
            OBX|23|TX|||plan and performed brachytherapy procedure as described.
        """.trimIndent()

        // messageMDMT02 processes at index 0
        val filePathMDMT02 =
            "/hl7v2/date=1990-01-03/tenant_id=mockTenant/message_type=MDM/message_event=MDMT02/06-07-42-999-0.json"
        justRun { mockClient.upload(filePathMDMT02, messageMDMT02) }
        assertTrue(service.publishHL7v2(tenantId, listOf(messageMDMT02)))
        verify(exactly = 1) { mockClient.upload(any(), any()) }
    }

    @Test
    fun `HL7v2 bad message structure halts processing of message list`() {
        mockkConstructor(LocalDate::class)
        mockkStatic(LocalDate::class)
        val mockkLocalDate = mockk<LocalDate> {
            every { format(DateTimeFormatter.ISO_LOCAL_DATE) } returns "1990-01-03"
        }
        every { LocalDate.now() } returns mockkLocalDate

        mockkConstructor(LocalTime::class)
        mockkStatic(LocalTime::class)
        val mockkLocalTime = mockk<LocalTime> {
            every { format(DateTimeFormatter.ISO_LOCAL_TIME) } returns "06:07:42.999"
        }
        every { LocalTime.now() } returns mockkLocalTime

        val messageBadData = "MSH|bad data"
        val messageMDMT02 = """
            MSH|^~\&|Brocade|MDACC|Epic|MDACC|20220412161126||MDM^T02|121160|T|2.7
            EVN|T02|20220412161126
            PID|||2008661^^^EPI^MR||RADONCTEST^GISELLE||19671028|F||||||
            PV1||I
            TXA||101||20220412000000|13765^CHUNG^CAROLINE^^^^^^PROVID^^^^PROVID|20220412161126|20220412161126||13765^CHUNG^CAROLINE^^^^^^PROVID^^^^PROVID|||^^dpg121160_637853766870444105|||||IP||UN|||||||
            OBX|1|TX|||Date of Service: Tuesday, April 12, 2022
            OBX|2|TX|||
            OBX|3|TX|||Diagnosis: Malignant neoplasm of cervix uteri, unspecified
            OBX|4|TX|||Histopathology: 8070/3 Squamous cell carcinoma, NOS
        """.trimIndent()

        // messageBadData is skipped at index 0, throws exception
        val exception = assertThrows<IllegalStateException> {
            service.publishHL7v2(tenantId, listOf(messageBadData, messageMDMT02))
        }
        Assertions.assertEquals(
            "Did not publish HL7v2 message to datalake for tenant $tenantId: the message has invalid structure",
            exception.message
        )
    }

    @Test
    fun `HL7v2 ADTA01 message is skipped as not an EventType Ronin deals with, but MDMT02 and MDMT06 process`() {
        mockkConstructor(LocalDate::class)
        mockkStatic(LocalDate::class)
        val mockkLocalDate = mockk<LocalDate> {
            every { format(DateTimeFormatter.ISO_LOCAL_DATE) } returns "1990-01-03"
        }
        every { LocalDate.now() } returns mockkLocalDate

        mockkConstructor(LocalTime::class)
        mockkStatic(LocalTime::class)
        val mockkLocalTime = mockk<LocalTime> {
            every { format(DateTimeFormatter.ISO_LOCAL_TIME) } returns "06:07:42.999"
        }
        every { LocalTime.now() } returns mockkLocalTime

        val messageMDMT02 = """
            MSH|^~\&|Brocade|MDACC|Epic|MDACC|20220412161126||MDM^T02|121160|T|2.7
            EVN|T02|20220412161126
            PID|||2008661^^^EPI^MR||RADONCTEST^GISELLE||19671028|F||||||
            PV1||I
            TXA||101||20220412000000|13765^CHUNG^CAROLINE^^^^^^PROVID^^^^PROVID|20220412161126|20220412161126||13765^CHUNG^CAROLINE^^^^^^PROVID^^^^PROVID|||^^dpg121160_637853766870444105|||||IP||UN|||||||
            OBX|1|TX|||Date of Service: Tuesday, April 12, 2022
            OBX|2|TX|||
            OBX|3|TX|||Diagnosis: Malignant neoplasm of cervix uteri, unspecified
            OBX|4|TX|||Histopathology: 8070/3 Squamous cell carcinoma, NOS
        """.trimIndent()

        val messageADTA01 = """
            MSH|^~\&|HIS|RIH|EKG|EKG|199904140038||ADT^A01||P|2.2
            PID|0001|00009874|00001122|A00977|SMITH^JOHN^M|MOM|19581119|F|NOTREAL^LINDA^M|C|564 SPRING ST^^NEEDHAM^MA^02494^US|0002|(818)565-1551|(425)828-3344|E|S|C|0000444444|252-00-4414||||SA|||SA||||NONE|V1|0001|I|D.ER^50A^M110^01|ER|P00055|11B^M011^02|070615^BATMAN^GEORGE^L|555888^NOTREAL^BOB^K^DR^MD|777889^NOTREAL^SAM^T^DR^MD^PHD|ER|D.WT^1A^M010^01|||ER|AMB|02|070615^NOTREAL^BILL^L|ER|000001916994|D||||||||||||||||GDD|WA|NORM|02|O|02|E.IN^02D^M090^01|E.IN^01D^M080^01|199904072124|199904101200|199904101200||||5555112333|||666097^NOTREAL^MANNY^P
            NK1|0222555|NOTREAL^JAMES^R|FA|STREET^OTHER STREET^CITY^ST^55566|(222)111-3333|(888)999-0000|||||||ORGANIZATION
            PV1|0001|I|D.ER^1F^M950^01|ER|P000998|11B^M011^02|070615^BATMAN^GEORGE^L|555888^OKNEL^BOB^K^DR^MD|777889^NOTREAL^SAM^T^DR^MD^PHD|ER|D.WT^1A^M010^01|||ER|AMB|02|070615^VOICE^BILL^L|ER|000001916994|D||||||||||||||||GDD|WA|NORM|02|O|02|E.IN^02D^M090^01|E.IN^01D^M080^01|199904072124|199904101200|||||5555112333|||666097^DNOTREAL^MANNY^P
            PV2|||0112^TESTING|55555^PATIENT IS NORMAL|NONE|||19990225|19990226|1|1|TESTING|555888^NOTREAL^BOB^K^DR^MD||||||||||PROD^003^099|02|ER||NONE|19990225|19990223|19990316|NONE
            AL1||SEV|001^POLLEN
            GT1||0222PL|NOTREAL^BOB^B||STREET^OTHER STREET^CITY^ST^77787|(444)999-3333|(222)777-5555||||MO|111-33-5555||||NOTREAL GILL N|STREET^OTHER STREET^CITY^ST^99999|(111)222-3333
            IN1||022254P|4558PD|BLUE CROSS|STREET^OTHER STREET^CITY^ST^00990||(333)333-6666||221K|LENIX|||19980515|19990515|||PATIENT01 TEST D||||||||||||||||||02LL|022LP554
        """.trimIndent()

        val messageMDMT06 = """
            MSH|^~\&|Brocade|MDACC|Epic|MDACC|20220413161126||MDM^T06|121160|T|2.7
            EVN|T06|20220413161126
            PID|||2008661^^^EPI^MR||RADONCTEST^GISELLE||19671028|F||||||
            PV1||I
            TXA||101||20220413000000|13765^CHUNG^CAROLINE^^^^^^PROVID^^^^PROVID|20220413161126|20220413161126||13765^CHUNG^CAROLINE^^^^^^PROVID^^^^PROVID|||^^dpg121160_637853766870444105|||||IP||UN|||||||
            OBX|1|TX|||Date of Service: Wednesday, April 13, 2022
            OBX|2|TX|||
            OBX|3|TX|||Diagnosis: Malignant neoplasm of cervix uteri, unspecified
            OBX|4|TX|||Histopathology: 8070/3 Squamous cell carcinoma, NOS
        """.trimIndent()

        // messageMDMT02 processes at index 0, messageADTA01 is an unsupported type at index 1, messageMDMT06 processes at index 2
        val filePathString = "/hl7v2/date=1990-01-03/tenant_id=mockTenant__REPLACE__.json"
        val filePathMDMT02 = filePathString.replace("__REPLACE__", "/message_type=MDM/message_event=MDMT02/06-07-42-999-0")
        val filePathADTA01 = filePathString.replace("__REPLACE__", "/message_type=ADT/message_event=ADTA01/06-07-42-999-1")
        val filePathMDMT06 = filePathString.replace("__REPLACE__", "/message_type=MDM/message_event=MDMT06/06-07-42-999-2")

        justRun { mockClient.upload(filePathMDMT02, messageMDMT02) }
        justRun { mockClient.upload(filePathADTA01, messageADTA01) }
        justRun { mockClient.upload(filePathMDMT06, messageMDMT06) }
        assertTrue(service.publishHL7v2(tenantId, listOf(messageMDMT02, messageADTA01, messageMDMT06)))
        verify(exactly = 2) { mockClient.upload(any(), any()) }
    }

    @AfterEach
    fun unmockk() {
        unmockkAll()
    }
}
