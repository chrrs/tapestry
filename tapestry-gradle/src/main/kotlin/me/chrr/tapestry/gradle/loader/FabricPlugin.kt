package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateFabricManifestTask
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

class FabricPlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override fun applyLoaderPlugin() {
        super.applyJavaPlugin()
        super.applyAnnotationProcessor()
        super.preferPlatformAttribute("fabric")

        // Apply Fabric Loom to build the mod.
        target.plugins.apply(LoomNoRemapGradlePlugin::class.java)
        val loom = target.the<LoomGradleExtensionAPI>()

        loom.mods.create(tapestry.info.id.get()) {
            sourceSets.forEach { sourceSet(it.get()) }
        }

        target.repositories.add(target.repositories.maven("https://maven.fabricmc.net/"))
        target.dependencies.add("minecraft", "com.mojang:minecraft:${tapestry.versions.minecraft.get()}")
        target.dependencies.add("implementation", "net.fabricmc:fabric-loader:${tapestry.versions.fabricLoader.get()}")

        // Automatically generate fabric.mod.json from tapestry extension.
        val generateManifest = target.tasks.register<GenerateFabricManifestTask>("generateFabricModJson") {
            info.set(tapestry.info)
            outputFile.set(generatedResourcesDir.map { it.file("fabric.mod.json") })
        }

        target.tasks.named("generateResources") { dependsOn(generateManifest) }

        // Make sure the JAR file is named correctly.
        target.tasks.named<Jar>("jar") {
            tapestry.applyArchiveName(this, "fabric")
        }

        // Generate run configurations.
        if (!tapestry.isCI()) {
            val relativeRunDir = tapestry.game.runDir.get().asFile.relativeTo(target.projectDir)

            loom.runs {
                if (tapestry.info.environment.get() != Environment.Server)
                    named("client") {
                        isIdeConfigGenerated = false
                        tapestry.game.username.ifPresent { programArgs("--username", it) }
                        runDir = relativeRunDir.path
                        environment = "client"
                    }

                if (tapestry.info.environment.get() != Environment.Client)
                    named("server") {
                        isIdeConfigGenerated = false
                        runDir = relativeRunDir.path + "/server"
                        environment = "server"
                    }
            }
        }
    }

    override fun addBuildDependency(other: LoaderPlugin) {
        target.dependencies.add("api", other.target)

        target.tasks.named<Jar>("jar") { from(other.sourceSets.map { set -> set.map { it.output } }) }
        target.tasks.named<Jar>("sourcesJar") { from(other.sourceSets.map { set -> set.map { it.allSource } }) }

        val loom = target.the<LoomGradleExtensionAPI>()
        loom.mods.configureEach { other.sourceSets.forEach { sourceSet(it.get()) } }

        // FIXME: port over JiJ.
    }
}