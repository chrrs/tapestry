package me.chrr.tapestry.gradle

import me.chrr.tapestry.gradle.loader.FabricPlugin
import me.chrr.tapestry.gradle.loader.LoaderPlugin
import me.chrr.tapestry.gradle.loader.NeoForgePlugin
import me.chrr.tapestry.gradle.loader.NeoFormPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class TapestryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val tapestry = target.extensions.create<TapestryExtension>("tapestry")

        // fixme: move '@PlatformImplementation' to tapestry-base?
        // fixme: platforms should use some kind of loader-specific services?
        // idea: some kind of '@Initializer' annotation that calls a static method
        // idea: an 'updateVersions' task that auto-updates build.gradle.kts

        target.afterEvaluate {
            val versionTag = when {
                tapestry.isCI() -> "-local"
                System.getenv("RELEASE") == "true" -> ""
                else -> "-snapshot"
            }
            tapestry.info.version.set("${tapestry.info.version.get()}$versionTag")

            if (!tapestry.game.runDir.isPresent)
                tapestry.game.runDir.set(target.projectDir.resolve("run"))

            val plugins = applyLoaderPlugins(target, tapestry)
            createMergedJarTask(target, plugins.map { it.target }, tapestry)
        }
    }

    fun applyLoaderPlugins(target: Project, tapestry: TapestryExtension): List<LoaderPlugin> {
        // Create NeoForm plugin for all common projects.
        val common = tapestry.projects.common.map { it.ifEmpty { listOf(target) } }
        val commonPlugins = common.get().map { NeoFormPlugin(tapestry, it) }

        // Create loader-specific plugin for all loader projects.
        val loaderPlugins = mutableListOf<LoaderPlugin>()

        val fabric = tapestry.projects.common.map { it.ifEmpty { listOf(target.project("fabric")) } }
        if (tapestry.versions.fabricLoader.isPresent)
            loaderPlugins.addAll(fabric.get().map { FabricPlugin(tapestry, it) })

        val neoforge = tapestry.projects.common.map { it.ifEmpty { listOf(target.project("neoforge")) } }
        if (tapestry.versions.neoforge.isPresent)
            loaderPlugins.addAll(neoforge.get().map { NeoForgePlugin(tapestry, it) })

        // Apply the common plugins.
        for (plugin in commonPlugins)
            plugin.applyAfterEvaluate()

        // Apply the loader plugins and depend on the common projects.
        for (plugin in loaderPlugins) {
            plugin.applyAfterEvaluate()
            commonPlugins.forEach { plugin.addBuildDependency(it) }
        }

        return listOf(commonPlugins, loaderPlugins).flatten()
    }

    fun createMergedJarTask(root: Project, projects: List<Project>, tapestry: TapestryExtension) =
        root.tasks.register<Jar>("buildMergedJar") {
            group = "build"
            description = "Compiles for all loaders and generates a universal mod jar."

            archiveFileName.set(tapestry.createArchiveFileName(""))
            manifest.attributes("Tapestry-Merged-Jar" to "true")
            duplicatesStrategy = DuplicatesStrategy.FAIL

            val classes = projects.map { it.tasks.named("classes") }
            dependsOn(classes)

            val sourceSets = projects.map { it.extensions.getByType<SourceSetContainer>().getByName("main").output }
            from(sourceSets)
        }
}