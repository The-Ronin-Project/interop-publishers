plugins {
    id("com.projectronin.interop.gradle.base")
    id("com.projectronin.interop.gradle.integration") apply false
    id("com.projectronin.interop.gradle.ktor") apply false
    id("com.projectronin.interop.gradle.mockk") apply false
    id("com.projectronin.interop.gradle.publish") apply false
    id("com.projectronin.interop.gradle.spring") apply false
}

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.publish")
}
