plugins {
    id("com.projectronin.interop.gradle.spring")
    id("com.projectronin.interop.gradle.mockk")
    id("com.projectronin.interop.gradle.ktor")
}

dependencies {
    // Spring
    implementation("org.springframework:spring-context")
}
