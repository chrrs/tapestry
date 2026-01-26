package me.chrr.tapestry.gradle.platform

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateFabricManifestTask
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class FabricPlatform(tapestry: TapestryExtension, target: Project) : LoaderPlatform(tapestry, target) {
    override val type = PlatformType.Fabric
    override val jijConfigurationName = "include"

    override fun applyPlatformPlugin() {
        super.applyPlatformPlugin()
        super.preferPlatformAttribute("fabric")

        // Apply Fabric Loom to build the mod.
        target.plugins.apply(LoomNoRemapGradlePlugin::class)
        val loom = target.the<LoomGradleExtensionAPI>()

        loom.mods.register(tapestry.info.id.get()) {
            sourceSets.get().forEach { sourceSet(it) }
            commonSourceSets.get().forEach { sourceSet(it) }
        }

        // Add the Minecraft and Fabric Loader dependencies.
        target.repositories.add(target.repositories.maven("https://maven.fabricmc.net/"))
        target.dependencies {
            "minecraft"(tapestry.versions.minecraft.map { "com.mojang:minecraft:$it" })
            "implementation"(tapestry.versions.fabricLoader.map { "net.fabricmc:fabric-loader:$it" })
        }

        // Register the class tweaker specified in the tapestry extension.
        loom.accessWidenerPath.set(target.layout.file(super.findResource(tapestry.transform.classTweaker)))

        // Automatically generate fabric.mod.json from tapestry extension.
        target.tasks.register<GenerateFabricManifestTask>("generateFabricModJson") {
            tapestry.set(this@FabricPlatform.tapestry)
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