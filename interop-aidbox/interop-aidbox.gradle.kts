plugins {
    id("com.projectronin.interop.gradle.jackson")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop.fhir:interop-fhir:${project.property("interopFhirVersion")}")

    testImplementation("com.projectronin.interop.ehr:interop-ehr:${project.property("interopEhrVersion")}")
    testImplementation("com.projectronin.interop:interop-common-jackson:${project.property("interopCommonVersion")}")
}
