plugins {
    id("me.chrr.tapestry.gradle")
}

tapestry {
    versions {
        minecraft = rootProject.prop("minecraft.version")
        fabricLoader = rootProject.prop("fabric.loader.version")
        neoforge = rootProject.prop("neoforge.version")
    }

    info {
        id = "tapestry"
        version = "${rootProject.version}"

        name = "Tapestry"
        description = "Useful utilities for multi-loader mod development."
        authors = listOf("chrrrs")
        license = "MIT"

        url = "https://github.com/chrrs/tapestry"
        sources = "https://github.com/chrrs/tapestry"
        issues = "https://github.com/chrrs/tapestry/issues"

        icon = "assets/tapestry/icon.png"
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
