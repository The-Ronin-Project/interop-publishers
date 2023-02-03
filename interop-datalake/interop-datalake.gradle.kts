plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.integration")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.fhir)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.oci.common)
    implementation(libs.oci.objectstorage)
    implementation(libs.oci.http.client)

    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")

    itImplementation(project(":interop-aidbox-testcontainer"))
    itImplementation("org.testcontainers:junit-jupiter")
    itImplementation(libs.mockserver.client.java)
}
