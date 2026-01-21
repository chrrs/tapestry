package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateFabricManifestTask
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

class FabricPlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override fun applyLoaderPlugin() {
        super.applyJavaPlugin("fabric")
        super.preferPlatformAttribute("fabric")
        super.applyAnnotationProcessor()

        // Apply Fabric Loom to build the mod.
        target.plugins.apply(LoomNoRemapGradlePlugin::class.java)
        val loom = target.the<LoomGradleExtensionAPI>()

        loom.mods.register(tapestry.info.id.get()) {
            ownSourceSets.forEach { sourceSet(it.get()) }
            otherSourceSets.forEach { sourceSet(it.get()) }
        }

        target.repositories.add(target.repositories.maven("https://maven.fabricmc.net/"))
        target.dependencies.add("minecraft", "com.mojang:minecraft:${tapestry.versions.minecraft.get()}")
        target.dependencies.add("implementation", "net.fabricmc:fabric-loader:${tapestry.versions.fabricLoader.get()}")

        // Include any dependencies in the JiJ configuration.
        target.configurations.named("include") {
            dependencies.addAllLater(target.configurations.named("jij").map { it.incoming.dependencies })
        }

        // Register the class tweaker specified in the tapestry extension.
        loom.accessWidenerPath.set(target.layout.file(super.findResource(tapestry.transform.classTweaker)))

        // Automatically generate fabric.mod.json from tapestry extension.
        target.tasks.register<GenerateFabricManifestTask>("generateFabricModJson") {
            tapestry.set(this@FabricPlugin.tapestry)
            outputFile.set(generatedResourcesDir.map { it.file("fabric.mod.json") })
        }.also { super.copyGeneratedResources(target.files(it)) }

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
}