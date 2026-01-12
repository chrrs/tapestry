package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateManifestTask
import net.neoforged.moddevgradle.boot.ModDevPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

class NeoForgePlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override fun applyAfterEvaluate() {
        val java = super.applyJavaPlugin()
        super.applyAnnotationProcessor()

        // Apply ModDevGradle to build the mod.
        target.plugins.apply(ModDevPlugin::class.java)
        val neoForge = target.the<NeoForgeExtension>()

        val main = java.sourceSets.getByName("main")
        neoForge.mods.create(tapestry.info.id.get()).sourceSet(main)

        neoForge.enable {
            version = tapestry.versions.neoforge.get()
            isDisableRecompilation = tapestry.isCI()
        }

        // Generate neoforge.mods.toml when processing resources.
        val generateManifest = target.tasks.register<GenerateManifestTask>(
            "generateNeoForgeModsToml", tapestry, Loader.NeoForge
        )

        target.tasks.named("processResources") { dependsOn(generateManifest) }

        // Make sure the JAR file is named correctly.
        target.tasks.named<Jar>("jar") {
            tapestry.applyArchiveName(this, "neoforge")
        }

        // Generate run configurations.
        if (!tapestry.isCI())
            neoForge.runs {
                if (tapestry.info.environment.get() != Environment.Server)
                    create("client") {
                        ideFolderName.set(tapestry.info.name)
                        ideName.set("NeoForge Client [${tapestry.info.id.get()}]")
                        if (!tapestry.game.generateIdeConfig.get()) disableIdeRun()

                        tapestry.game.username.ifPresent { programArguments.addAll("--username", it) }
                        gameDirectory.set(tapestry.game.runDir)
                        client()
                    }

                if (tapestry.info.environment.get() != Environment.Client)
                    create("server") {
                        ideFolderName.set(tapestry.info.name)
                        ideName.set("NeoForge Server [${tapestry.info.id.get()}]")
                        if (!tapestry.game.generateIdeConfig.get()) disableIdeRun()

                        gameDirectory.set(tapestry.game.runDir.map { it.dir("server") })
                        server()
                    }
            }
    }

    override fun addBuildDependency(other: LoaderPlugin) {
        target.dependencies.add("compileOnly", other.target)

        val sourceSets = other.target.the<SourceSetContainer>()
        target.tasks.named<Jar>("jar") { from(sourceSets.getByName("main").output) }
        target.tasks.named<Jar>("sourcesJar") { from(sourceSets.getByName("main").allSource) }

        // FIXME: port over JiJ.
    }
}