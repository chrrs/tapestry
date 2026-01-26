package me.chrr.tapestry.gradle.platform

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.classtweaker.ConvertClassTweakersTask
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateNeoForgeManifestTask
import net.neoforged.moddevgradle.boot.ModDevPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

class NeoForgePlatform(tapestry: TapestryExtension, target: Project) : LoaderPlatform(tapestry, target) {
    override val type = PlatformType.NeoForge
    override val jijConfigurationName = "jarJar"

    override fun applyPlatformPlugin() {
        super.applyPlatformPlugin()
        super.preferPlatformAttribute("neoforge")

        // Apply ModDevGradle to build the mod.
        target.plugins.apply(ModDevPlugin::class.java)
        val neoForge = target.the<NeoForgeExtension>()

        neoForge.mods.register(tapestry.info.id.get()) {
            sourceSets.get().forEach { sourceSet(it) }
            commonSourceSets.get().forEach { sourceSet(it) }
        }

        neoForge.enable {
            version = tapestry.versions.neoforge.get()
            isDisableRecompilation = tapestry.isCI()
        }

        // Convert any class tweakers to access transformers, and register them.
        val accessTransformer = target.tasks.register<ConvertClassTweakersTask>("convertClassTweaker") {
            if (tapestry.transform.classTweaker.isPresent) {
                inputFiles.add(findResource(tapestry.transform.classTweaker))
                outputFiles.add(generatedResourcesDir.map { it.file("META-INF/accesstransformer.cfg").asFile })
            }
        }

        neoForge.accessTransformers.from(accessTransformer)
        super.copyGeneratedResources(target.files(accessTransformer))

        // Automatically generate neoforge.mods.toml from tapestry extension.
        target.tasks.register<GenerateNeoForgeManifestTask>("generateNeoForgeModsToml") {
            tapestry.set(this@NeoForgePlatform.tapestry)
            outputFile.set(generatedResourcesDir.map { it.dir("META-INF").file("neoforge.mods.toml") })
        }.also { super.copyGeneratedResources(target.files(it)) }

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
}