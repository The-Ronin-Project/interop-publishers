plugins {
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop.fhir:interop-fhir:${project.property("interopFhirVersion")}")
    implementation(project(":interop-aidbox"))
}
