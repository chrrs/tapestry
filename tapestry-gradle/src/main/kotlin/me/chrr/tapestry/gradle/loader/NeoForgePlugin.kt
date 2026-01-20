package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateNeoForgeManifestTask
import net.neoforged.moddevgradle.boot.ModDevPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

class NeoForgePlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override fun applyLoaderPlugin() {
        super.applyJavaPlugin()
        super.applyAnnotationProcessor()
        super.preferPlatformAttribute("neoforge")

        // Apply ModDevGradle to build the mod.
        target.plugins.apply(ModDevPlugin::class.java)
        val neoForge = target.the<NeoForgeExtension>()

        neoForge.mods.create(tapestry.info.id.get()) {
            sourceSets.forEach { sourceSet(it.get()) }
        }

        neoForge.enable {
            version = tapestry.versions.neoforge.get()
            isDisableRecompilation = tapestry.isCI()
        }

        // Automatically generate neoforge.mods.toml from tapestry extension.
        val generateManifest = target.tasks.register<GenerateNeoForgeManifestTask>("generateNeoForgeModsToml") {
            info.set(tapestry.info)
            outputFile.set(generatedResourcesDir.map { it.dir("META-INF").file("neoforge.mods.toml") })
        }

        target.tasks.named("generateResources") { dependsOn(generateManifest) }

        // Make sure the JAR file is named correctly.
        target.tasks.named<Jar>("jar") {
            tapestry.applyArchiveName(this, "neoforge")
        }

        // Generate run configurations.
        if (!tapestry.isCI())
            neoForge.runs {
                if (tapestry.info.environment.get() != Environment.Server)
                    create("client") {
                        disableIdeRun()
                        tapestry.game.username.ifPresent { programArguments.addAll("--username", it) }
                        gameDirectory.set(tapestry.game.runDir)
                        client()
                    }

                if (tapestry.info.environment.get() != Environment.Client)
                    create("server") {
                        disableIdeRun()
                        gameDirectory.set(tapestry.game.runDir.map { it.dir("server") })
                        server()
                    }
            }
    }

    override fun addBuildDependency(other: LoaderPlugin) {
        target.dependencies.add("api", other.target)

        target.tasks.named<Jar>("jar") { from(other.sourceSets.map { set -> set.map { it.output } }) }
        target.tasks.named<Jar>("sourcesJar") { from(other.sourceSets.map { set -> set.map { it.allSource } }) }

        val neoForge = target.the<NeoForgeExtension>()
        neoForge.mods.configureEach { other.sourceSets.forEach { sourceSet(it.get()) } }

        // FIXME: port over JiJ.
    }
}