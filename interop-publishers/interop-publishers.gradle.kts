plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.fhir)
    implementation(project(":interop-aidbox"))

    testImplementation(libs.mockk)
}
