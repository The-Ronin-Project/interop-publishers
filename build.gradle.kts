plugins {
    id("com.projectronin.interop.gradle.publish")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.fhir)
    implementation(libs.event.interop.resource.internal)
    implementation(libs.interop.datalake)
    implementation(libs.ehr.data.authority.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.interop.kafka)

    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")
}
