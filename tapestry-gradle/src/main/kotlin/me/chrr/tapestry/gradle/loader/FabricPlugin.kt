package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.Environment
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.manifest.GenerateManifestTask
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.register

class FabricPlugin(tapestry: TapestryExtension, target: Project) : LoaderPlugin(tapestry, target) {
    override fun applyAfterEvaluate() {
        val java = super.applyJavaPlugin()
        super.applyAnnotationProcessor()

        // Apply Fabric Loom to build the mod.
        target.plugins.apply(LoomNoRemapGradlePlugin::class.java)
        val loom = target.extensions.getByType<LoomGradleExtensionAPI>()

        val main = java.sourceSets.getByName("main")
        loom.mods.create(tapestry.info.id.get()).sourceSet(main)

        target.repositories.add(target.repositories.maven("https://maven.fabricmc.net/"))
        target.dependencies.add("minecraft", "com.mojang:minecraft:${tapestry.versions.minecraft.get()}")
        target.dependencies.add("implementation", "net.fabricmc:fabric-loader:${tapestry.versions.fabricLoader.get()}")

        // Generate fabric.mod.json when processing resources.
        val generateManifest = target.tasks.register<GenerateManifestTask>(
            "generateFabricModJson", tapestry, Loader.Fabric
        )

        target.tasks.getByName("processResources").dependsOn(generateManifest)

        // Make sure the JAR file is named correctly.
        target.tasks.getByName<Jar>("jar") {
            tapestry.applyArchiveName(this, "fabric")
        }

        // Generate run configurations.
        if (!tapestry.isCI()) {
            val relativeRunDir = tapestry.game.runDir.get().asFile.relativeTo(target.projectDir)

            loom.runs {
                if (tapestry.info.environment.get() != Environment.Server)
                    named("client") {
                        ideConfigFolder.set(tapestry.info.name)
                        configName = "Fabric Client [${tapestry.info.id.get()}]"
                        appendProjectPathToConfigName.set(false)
                        isIdeConfigGenerated = tapestry.game.generateIdeConfig.get()

                        tapestry.game.username.ifPresent { programArgs("--username", it) }
                        runDir = relativeRunDir.path
                        environment = "client"
                    }

                if (tapestry.info.environment.get() != Environment.Client)
                    named("server") {
                        ideConfigFolder.set(tapestry.info.name)
                        configName = "Fabric Server [${tapestry.info.id.get()}]"
                        appendProjectPathToConfigName.set(false)
                        isIdeConfigGenerated = tapestry.game.generateIdeConfig.get()

                        runDir = relativeRunDir.path + "/server"
                        environment = "server"
                    }
            }
        }
    }

    override fun addBuildDependency(other: LoaderPlugin) {
        target.dependencies.add("compileOnly", other.target)

        val sourceSets = other.target.extensions.getByType<SourceSetContainer>()
        target.tasks.getByName<Jar>("jar").from(sourceSets.getByName("main").output)
        target.tasks.getByName<Jar>("sourcesJar").from(sourceSets.getByName("main").allSource)

        // FIXME: port over JiJ.
    }
}