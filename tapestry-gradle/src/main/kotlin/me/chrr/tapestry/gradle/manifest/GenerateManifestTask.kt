package me.chrr.tapestry.gradle.manifest

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.the
import java.io.File

abstract class GenerateManifestTask : DefaultTask() {
    init {
        dependsOn("compileJava")
    }

    @TaskAction
    fun generate() {
        // Read the output of the annotation processor.
        val fabricEntrypoints = mutableMapOf<String, MutableList<String>>()
        val platformImplementations = mutableMapOf<String, MutableList<String>>()

        val sourceSets = project.the<SourceSetContainer>()
        for (sourceDir in sourceSets.flatMap { it.output.generatedSourcesDirs }) {
            val tapestryDir = sourceDir.resolve("tapestry")
            readPairsToMap(tapestryDir.resolve("entrypoints.txt"), fabricEntrypoints)
            readPairsToMap(tapestryDir.resolve("platforms.txt"), platformImplementations)
        }

        // Actually generate the manifest.
        generateManifest(Context(fabricEntrypoints, platformImplementations))
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

    protected abstract fun generateManifest(ctx: Context)

    data class Context(
        val fabricEntrypoints: Map<String, List<String>>,
        val platformImplementations: Map<String, List<String>>,
    )
}