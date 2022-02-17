plugins {
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.ktor")
    id("com.projectronin.interop.gradle.spring")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.16.2")
    }
}

dependencies {
    implementation("org.testcontainers:junit-jupiter")
    implementation("commons-codec:commons-codec:1.15")
}
