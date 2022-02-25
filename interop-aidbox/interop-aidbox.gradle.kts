plugins {
    id("com.projectronin.interop.gradle.jackson")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.integration")
}

dependencies {
    implementation("com.projectronin.interop:interop-common:${project.property("interopCommonVersion")}")
    implementation("com.projectronin.interop.fhir:interop-fhir:${project.property("interopFhirVersion")}")

    testImplementation("com.projectronin.interop:interop-common-jackson:${project.property("interopCommonVersion")}")

    itImplementation(project(":interop-aidbox-testcontainer"))
    itImplementation("org.testcontainers:junit-jupiter")
    itImplementation("org.springframework:spring-test")
}
