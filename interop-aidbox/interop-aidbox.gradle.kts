plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.integration")
}

dependencies {
    implementation(libs.dd.trace.api)
    implementation(libs.interop.common)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.fhir)

    testImplementation(libs.interop.commonJackson)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")

    itImplementation(project(":interop-aidbox-testcontainer"))
    itImplementation("org.testcontainers:junit-jupiter")
    itImplementation(libs.interop.commonHttp)
    itImplementation(libs.interop.fhir)
    itImplementation(platform(libs.spring.boot.parent))
    itImplementation("org.springframework:spring-test")
    itImplementation(platform("org.springframework:spring-framework-bom:5.3.23"))
    itImplementation("org.springframework:spring-context")
    itImplementation(libs.mockk)
}
