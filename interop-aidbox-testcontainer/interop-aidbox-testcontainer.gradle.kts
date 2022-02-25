plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
}

dependencies {
    api(platform("org.testcontainers:testcontainers-bom:1.16.0"))
    implementation("org.testcontainers:junit-jupiter")
    implementation("commons-codec:commons-codec:1.15")
}
