package me.chrr.tapestry.gradle.classtweaker

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import java.io.File

@CacheableTask
open class ConvertClassTweakersTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
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