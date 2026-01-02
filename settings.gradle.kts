rootProject.name = "tapestry"

pluginManagement {
    includeBuild("tapestry-gradle")
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

fun includeMod(name: String) = include(name, "$name:fabric", "$name:neoforge")
includeMod("tapestry-base")
includeMod("tapestry-config")
