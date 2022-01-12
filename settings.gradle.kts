rootProject.name = "interop-publishers-build"

include("interop-aidbox")
include("interop-aidbox-testcontainer")

for (project in rootProject.children) {
    project.buildFileName = "${project.name}.gradle.kts"
}

pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/projectronin/package-repo")
            credentials {
                username = System.getenv("PACKAGE_USER")
                password = System.getenv("PACKAGE_TOKEN")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
