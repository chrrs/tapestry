package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateNeoForgeManifestTask
import net.neoforged.moddevgradle.boot.ModDevPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

class NeoForgePlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override fun applyLoaderPlugin() {
        super.applyJavaPlugin("neoforge")
        super.preferPlatformAttribute("neoforge")
        super.applyAnnotationProcessor()

        // Apply ModDevGradle to build the mod.
        target.plugins.apply(ModDevPlugin::class.java)
        val neoForge = target.the<NeoForgeExtension>()

        neoForge.mods.register(tapestry.info.id.get()) {
            ownSourceSets.forEach { sourceSet(it.get()) }
            otherSourceSets.forEach { sourceSet(it.get()) }
        }

        neoForge.enable {
            version = tapestry.versions.neoforge.get()
            isDisableRecompilation = tapestry.isCI()
        }

        // Include any dependencies in the JiJ configuration.
        target.configurations.named("jarJar") {
            dependencies.addAllLater(target.configurations.named("jij").map { it.incoming.dependencies })
        }

        // Convert any class tweakers to access transformers, and register them.
        super.createAccessTransformerTask().also {
            neoForge.accessTransformers.from(it)
            super.copyGeneratedResources(target.files(it))
        }

        // Automatically generate neoforge.mods.toml from tapestry extension.
        target.tasks.register<GenerateNeoForgeManifestTask>("generateNeoForgeModsToml") {
            tapestry.set(this@NeoForgePlugin.tapestry)
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