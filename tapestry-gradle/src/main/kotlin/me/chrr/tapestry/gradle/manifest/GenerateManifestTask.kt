package me.chrr.tapestry.gradle.manifest

import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.loader.Loader
import me.chrr.tapestry.gradle.tapestryBuildDir
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

abstract class GenerateManifestTask @Inject constructor(
    private val tapestry: TapestryExtension,
    private val loader: Loader,
) : DefaultTask() {
    @OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    private val entrypoints = mutableMapOf<String, MutableList<String>>()
    private val platforms = mutableMapOf<String, MutableList<String>>()

    init {
        // Read the output of the annotation processor.
        dependsOn("compileJava")
        project.tasks.getByName("compileJava").doLast {
            val sourceSets = project.extensions.getByType(SourceSetContainer::class)
            for (sourceDir in sourceSets.flatMap { it.output.generatedSourcesDirs }) {
                val tapestryDir = sourceDir.resolve("tapestry")
                readPairsToMap(tapestryDir.resolve("entrypoints.txt"), entrypoints)
                readPairsToMap(tapestryDir.resolve("platforms.txt"), platforms)
            }
        }

        // Set our output file to the right path depending on loader.
        outputFile.convention(project.tapestryBuildDir.map {
            when (loader) {
                Loader.Fabric -> it.file("generated/fabric.mod.json")
                Loader.NeoForge -> it.file("generated/META-INF/neoforge.mods.toml")
            }
        })
    }

    @TaskAction
    fun generate() {
        val ctx = ManifestContext(tapestry, entrypoints, platforms)

        val manifest = when (loader) {
            Loader.Fabric -> FabricManifest.generate(ctx)
            Loader.NeoForge -> NeoForgeManifest.generate(ctx)
        }

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        Files.write(file.toPath(), manifest.toByteArray())
    }

    private fun readPairsToMap(file: File, map: MutableMap<String, MutableList<String>>) {
        if (file.isFile) {
            val entries = file.readText()
                .lines()
                .filter { it.isNotBlank() }
                .map { it.split(';', limit = 2) }
            for ((key, value) in entries)
                map.getOrPut(key) { mutableListOf() }.add(value)
        }
    }
}