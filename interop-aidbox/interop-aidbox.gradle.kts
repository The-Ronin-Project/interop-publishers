plugins {
    id("com.projectronin.interop.gradle.jackson")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.integration")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.fhir)

    testImplementation(libs.interop.commonJackson)

    itImplementation(project(":interop-aidbox-testcontainer"))
    itImplementation("org.testcontainers:junit-jupiter")
    itImplementation("org.springframework:spring-test")
}
