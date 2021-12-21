plugins {
    id("com.projectronin.interop.gradle.base") version "1.0.0-SNAPSHOT"
    id("com.projectronin.interop.gradle.ktor") version "1.0.0-SNAPSHOT" apply false
    id("com.projectronin.interop.gradle.mockk") version "1.0.0-SNAPSHOT" apply false
    id("com.projectronin.interop.gradle.publish") version "1.0.0-SNAPSHOT" apply false
    id("com.projectronin.interop.gradle.spring") version "1.0.0-SNAPSHOT" apply false
}

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.publish")
}
