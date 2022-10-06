plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    api(platform(libs.testcontainers.bom))
    api("org.testcontainers:testcontainers")
    implementation("org.testcontainers:junit-jupiter")
    implementation(libs.commons.codec)
    implementation(libs.interop.commonHttp)
    testImplementation(libs.interop.fhir)
}
