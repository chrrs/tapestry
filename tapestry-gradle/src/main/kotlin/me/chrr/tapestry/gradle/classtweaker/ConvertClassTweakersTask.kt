package me.chrr.tapestry.gradle.classtweaker

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import java.io.File

open class ConvertClassTweakersTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    val inputFiles = project.objects.listProperty<File>()

    @OutputFiles
    val outputFiles = project.objects.listProperty<File>()

    @TaskAction
    fun convert() {
        inputFiles.get().zip(outputFiles.get())
            .forEach { (inputFile, outputFile) ->
                inputFile.bufferedReader().use { reader ->
                    val contents = ClassTweakerConverter.toAccessTransformer(reader)
                    outputFile.parentFile.mkdirs()
                    outputFile.writeText(contents)
                }
            }
    }
}