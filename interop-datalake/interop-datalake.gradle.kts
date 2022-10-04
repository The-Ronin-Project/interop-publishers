plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.oci.common)
    implementation(libs.oci.objectstorage)
    testImplementation(libs.mockk)
}
