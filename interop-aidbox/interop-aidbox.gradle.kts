plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.integration")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.fhir)

    testImplementation(libs.interop.commonJackson)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)

    itImplementation(project(":interop-aidbox-testcontainer"))
    itImplementation("org.testcontainers:junit-jupiter")
    itImplementation("org.springframework:spring-test")
    itImplementation(libs.interop.commonHttp)
}
