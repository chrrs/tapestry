package me.chrr.tapestry.gradle.platform

import me.chrr.tapestry.gradle.TapestryExtension
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.the

class CommonPlatform(tapestry: TapestryExtension, target: Project) : Platform(tapestry, target) {
    override val type = PlatformType.Common

    override fun applyPlatformPlugin() {
        super.applyPlatformPlugin()
        super.preferPlatformAttribute("common")

        // Apply Fabric Loom for the Minecraft dependency.
        target.plugins.apply(LoomNoRemapGradlePlugin::class)
        val loom = target.the<LoomGradleExtensionAPI>()

        loom.runs.clear()
        loom.runConfigs.configureEach { ideConfigGenerated(false) }

        // Include Minecraft and Mixin dependencies. Mixin already exists for Fabric
        // and NeoForge, so we only need to add these to the common project.
        target.repositories.maven("https://repo.spongepowered.org/maven/")
        target.dependencies {
            "minecraft"(tapestry.versions.minecraft.map { "com.mojang:minecraft:$it" })
            "compileOnly"("org.spongepowered:mixin:0.8.7")
            "compileOnly"("io.github.llamalad7:mixinextras-common:0.5.3")
        }

        // Register the class tweaker specified in the tapestry extension.
        loom.accessWidenerPath.set(target.layout.file(super.findResource(tapestry.transform.classTweaker)))

        // Add Mixin to the dependencies. Both of these already exist for Fabric and NeoForge,
        // so we only need to add these to the common project.
        target.repositories.maven("https://repo.spongepowered.org/maven/")
        target.dependencies {
            "compileOnly"("org.spongepowered:mixin:0.8.7")
            "compileOnly"("io.github.llamalad7:mixinextras-common:0.5.3")
        }
    }
}