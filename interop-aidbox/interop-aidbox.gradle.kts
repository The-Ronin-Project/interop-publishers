plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.integration")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.fhir)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.jackson)

    testImplementation(libs.interop.commonJackson)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)

    itImplementation(project(":interop-aidbox-testcontainer"))
    itImplementation("org.testcontainers:junit-jupiter")
    itImplementation("org.springframework:spring-test")
}
