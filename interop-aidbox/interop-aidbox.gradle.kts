plugins {
    id("com.projectronin.interop.gradle.jackson")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop.ehr:interop-fhir:${project.property("interopEhrVersion")}")

    testImplementation("com.projectronin.interop.ehr:interop-ehr:${project.property("interopEhrVersion")}")
    testImplementation("com.projectronin.interop.ehr:interop-fhir:${project.property("interopEhrVersion")}")
}
