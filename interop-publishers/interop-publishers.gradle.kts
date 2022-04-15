plugins {
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.fhir)
    implementation(project(":interop-aidbox"))
}
