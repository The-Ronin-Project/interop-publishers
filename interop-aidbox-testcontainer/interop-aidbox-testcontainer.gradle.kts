plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    api(platform(libs.testcontainers.bom))
    implementation("org.testcontainers:junit-jupiter")
    implementation(libs.commons.codec)
}
