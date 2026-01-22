package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.TapestryExtension
import net.fabricmc.loom.LoomCompanionGradlePlugin
import net.neoforged.moddevgradle.boot.ModDevPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

class CommonPlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override val platform = Platform.Common

    override fun applyLoaderPlugin() {
        super.applyJavaPlugin("common")
        super.preferPlatformAttribute("common")

        val minecraft = tapestry.versions.minecraft.get()
        val neoForm = tapestry.versions.neoform.get()

        if (!neoForm.startsWith(minecraft))
            throw IllegalArgumentException("NeoForm $neoForm is incompatible with Minecraft $minecraft.")

        // Apply the Loom Companion plugin, so Loom can use the sources.
        target.plugins.apply(LoomCompanionGradlePlugin::class.java)

        // Apply ModDevGradle in Vanilla mode to put Minecraft on the classpath.
        target.plugins.apply(ModDevPlugin::class.java)
        val neoForge = target.the<NeoForgeExtension>()

        neoForge.enable {
            neoFormVersion = neoForm
            isDisableRecompilation = tapestry.isCI()
        }

        // Convert any class tweakers to access transformers, and register them.
        val convertClassTweakers = super.createAccessTransformerTask()
        neoForge.accessTransformers.from(convertClassTweakers)
    }

    override fun addPluginDependency(other: LoaderPlugin) =
        throw IllegalStateException("NeoForm (common) projects shouldn't depend on other projects.")
}