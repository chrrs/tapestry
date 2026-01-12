package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.TapestryExtension
import net.neoforged.moddevgradle.boot.ModDevPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the

class NeoFormPlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override fun applyAfterEvaluate() {
        super.applyJavaPlugin()

        val minecraft = tapestry.versions.minecraft.get()
        val neoForm = tapestry.versions.neoform.get()

        if (!neoForm.startsWith(minecraft))
            throw IllegalArgumentException("NeoForm $neoForm is incompatible with Minecraft $minecraft.")

        // Apply ModDevGradle in Vanilla mode to put Minecraft on the classpath.
        target.plugins.apply(ModDevPlugin::class.java)
        val neoForge = target.the<NeoForgeExtension>()

        neoForge.enable {
            neoFormVersion = neoForm
            isDisableRecompilation = tapestry.isCI()
        }

        // Make sure the JAR file is named correctly.
        target.tasks.named<Jar>("jar") {
            tapestry.applyArchiveName(this, "common")
        }
    }

    override fun addBuildDependency(other: LoaderPlugin) =
        throw IllegalStateException("NeoForm (common) projects shouldn't depend on other projects.")
}