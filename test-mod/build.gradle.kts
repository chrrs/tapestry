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
        id = "tapestry_test_mod"
        version = "${rootProject.version}"
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
}
