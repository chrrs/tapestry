group = "me.chrr.tapestry"
version = property("tapestry.version") as String

tasks.register<Copy>("collectJars") {
    group = "build"
    description = "Collect all jars from subprojects and put them in a single directory."

    from(subprojects.mapNotNull { it.tasks.findByName("mergedJar") })
    into(layout.buildDirectory.dir("libs"))
}