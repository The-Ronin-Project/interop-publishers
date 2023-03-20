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
    itImplementation(libs.oci.common)
    itImplementation(libs.interop.commonJackson)
    itImplementation(libs.oci.objectstorage)
    itImplementation(libs.oci.http.client)
    itImplementation(libs.interop.fhir)
    itImplementation(platform("org.springframework:spring-framework-bom:5.3.23"))
    itImplementation("org.springframework:spring-context")
    itImplementation(libs.mockk)
    itImplementation(libs.interop.common)
    itImplementation(libs.interop.commonJackson)
    itImplementation(libs.interop.fhir)
}
