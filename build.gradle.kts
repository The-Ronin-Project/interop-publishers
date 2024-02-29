plugins {
    alias(libs.plugins.interop.gradle.publish)
    alias(libs.plugins.interop.gradle.junit)
    alias(libs.plugins.interop.gradle.spring)
    alias(libs.plugins.interop.version.catalog)
    alias(libs.plugins.interop.gradle.sonarqube)
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.dd.trace.api)
    implementation(libs.interop.fhir)
    implementation(libs.event.interop.resource.internal)
    implementation(libs.interop.datalake)
    implementation(libs.ehr.data.authority.client)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.interop.kafka)

    testImplementation(libs.mockk)
    testImplementation("org.springframework:spring-test")
}
