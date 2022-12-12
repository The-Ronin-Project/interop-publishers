package com.projectronin.interop.aidbox.utils

const val AIDBOX_LIMITED_PRACTITIONER_IDS_QUERY = "query Practitioner(\$tenant:string, \$fhirId:string) {" +
    "    PractitionerList(identifier_list: [\$tenant, \$fhirId]) {" +
    "        id" +
    "        identifier{" +
    "            system" +
    "            type{" +
    "                text" +
    "            }" +
    "            value" +
    "        }" +
    "    }" +
    "}"
const val AIDBOX_PATIENT_FHIR_IDS_QUERY = "query(\$tenant:string!, \$identifiers:string!){" +
    "    PatientList(" +
    "        identifier: \$tenant" +
    "        identifier_list: [\$identifiers]" +
    "    )" +
    "    {" +
    "        id" +
    "        identifier{" +
    "            system" +
    "            value" +
    "        }" +
    "    }" +
    "}"
const val AIDBOX_LOCATION_FHIR_IDS_QUERY = "query(\$tenant:string!, \$identifiers:string!) {" +
    "    LocationList(" +
    "        identifier: \$tenant" +
    "        identifier_list: [\$identifiers]" +
    "    )" +
    "    {" +
    "        id" +
    "        identifier {" +
    "            system" +
    "            value" +
    "        }" +
    "    }" +
    "}"
const val AIDBOX_PRACTITIONER_FHIR_IDS_QUERY = "query(\$tenant:string!, \$identifiers:string!) {" +
    "    PractitionerList(" +
    "        identifier: \$tenant" +
    "        identifier_list: [\$identifiers]" +
    "    )" +
    "    {" +
    "        id" +
    "        identifier {" +
    "            system" +
    "            value" +
    "        }" +
    "    }" +
    "}"
const val AIDBOX_PATIENT_LIST_QUERY = "query PatientList(" +
    "    \$identifier: string," +
    "    ){" +
    "    PatientList(identifier: \$identifier) {" +
    "        identifier{" +
    "            system" +
    "            type{" +
    "                text" +
    "            }" +
    "            value" +
    "        }," +
    "        id" +
    "    }" +
    "}"
const val AIDBOX_PRACTITIONER_LIST_QUERY = "query PractitionerList(" +
    "    \$identifier: string," +
    "    \$count: integer," +
    "    \$page: integer" +
    "){" +
    "    PractitionerList(identifier: \$identifier, _count: \$count, _page: \$page) {" +
    "        identifier{" +
    "            system" +
    "            type{" +
    "                text" +
    "            }" +
    "            value" +
    "        }" +
    "        id" +
    "    }" +
    "}"
