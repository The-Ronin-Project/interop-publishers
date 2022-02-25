rootProject.name = "interop-publishers-build"

include("interop-publishers")
include("interop-aidbox")
include("interop-aidbox-testcontainer")

for (project in rootProject.children) {
    project.buildFileName = "${project.name}.gradle.kts"
}

pluginManagement {
    val interopGradleVersion: String by settings
    plugins {
        id("com.projectronin.interop.gradle.base") version interopGradleVersion
        id("com.projectronin.interop.gradle.integration") version interopGradleVersion
        id("com.projectronin.interop.gradle.ktor") version interopGradleVersion
        id("com.projectronin.interop.gradle.mockk") version interopGradleVersion
        id("com.projectronin.interop.gradle.publish") version interopGradleVersion
        id("com.projectronin.interop.gradle.spring") version interopGradleVersion
    }

    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
            mavenContent {
                releasesOnly()
            }
        }
        mavenLocal()
        gradlePluginPortal()
    }
}
