plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.integration")
}

dependencies {
    implementation(libs.interop.fhir)
    implementation(libs.interop.common)
    implementation(libs.ronin.kafka)
    implementation(libs.bundles.interop.kafka.events)

    implementation(platform(libs.spring.boot.parent))
    implementation("org.springframework.boot:spring-boot")

    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-starter")

    testImplementation(platform(libs.testcontainers.bom))
    itImplementation("org.testcontainers:junit-jupiter")
    implementation(libs.interop.commonJackson)
}
