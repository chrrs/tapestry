plugins {
    id("me.chrr.tapestry.gradle")
}

tapestry {
    versions {
        minecraft = rootProject.prop("minecraft.version")
        neoform = rootProject.prop("neoform.version")
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

    publish {
        readChangelogFrom(file("CHANGELOG.md"))
        mergedJar = true

        modrinth = "abc123"
        curseforge = "abc123"
    }
}