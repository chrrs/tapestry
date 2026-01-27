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
        id = "tapestry_test_mod"
        version = "0.0.0-alpha"
        name = "Tapestry Test Mod"
        license = "MIT"
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
        modrinth = "abc123"
        curseforge = "abc123"
    }
}
