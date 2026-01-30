plugins {
    id("me.chrr.tapestry.gradle")
    `maven-publish`
}

tapestry {
    versions {
        minecraft = rootProject.prop("minecraft.version")
        fabricLoader = rootProject.prop("fabric.loader.version")
        neoforge = rootProject.prop("neoforge.version")
    }

    info {
        id = "tapestry_config"
        version = "${rootProject.version}"

        name = "Tapestry Config"
        description = "Simple annotation-based config library."
        authors = listOf("chrrrs")
        license = "MIT"

        url = "https://github.com/chrrs/tapestry"
        sources = "https://github.com/chrrs/tapestry"
        issues = "https://github.com/chrrs/tapestry/issues"

        icon = "assets/tapestry_config/icon.png"
        parentMod = "tapestry"
        isLibrary = true
    }

    depends {
        minecraft = rootProject.prop("minecraft.compatible").map { it.split(",") }
    }

    game {
        runDir = file("../run")
        username = "chrrz"
    }
}

publishing {
    repositories {
        maven("https://repo.repsy.io/chrrs/releases") {
            name = "ChrrsReleases"
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }

    publications.register<MavenPublication>("mod") {
        from(components["tapestry"])
        groupId = rootProject.group as String
        artifactId = project.name
        version = tapestry.qualifiedVersion.get()
    }
}