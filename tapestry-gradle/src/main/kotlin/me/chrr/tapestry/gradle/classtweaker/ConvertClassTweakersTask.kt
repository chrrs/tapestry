package me.chrr.tapestry.gradle.classtweaker

import net.fabricmc.classtweaker.api.ClassTweakerReader
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor
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
                    val builder = AccessTransformerBuilder()
                    ClassTweakerReader.create(builder).read(reader, "")
                    val contents = builder.accessTransformer.build()

                    outputFile.parentFile.mkdirs()
                    outputFile.writeText(contents)
                }
            }
    }

    private class AccessTransformerBuilder : ClassTweakerVisitor {
        val accessTransformer = AccessTransformer()

        override fun visitHeader(namespace: String) {
            if (namespace != "official")
                throw IllegalArgumentException("Class Tweaker namespace must be 'official'")
        }

        override fun visitInjectedInterface(owner: String, iface: String, transitive: Boolean) =
            throw NotImplementedError("Injected interfaces in Class Tweakers are not supported")

        // Access wideners are not exactly mappable to access transformers, so we need
        // to mimic their behaviour somewhat. See https://wiki.fabricmc.net/tutorial:accesswidening.
        //
        // Note that this will always remain lossy, so we over-widen some things to make sure it's
        // always compatible with what the access widener would produce.
        override fun visitAccessWidener(owner: String) =
            object : AccessWidenerVisitor {
                val className = owner.replace('/', '.')

                override fun visitClass(access: AccessWidenerVisitor.AccessType, transitive: Boolean) =
                    when (access) {
                        AccessWidenerVisitor.AccessType.ACCESSIBLE ->
                            accessTransformer.clazz(className)
                                .merge(AccessTransformer.Visibility.Public)

                        AccessWidenerVisitor.AccessType.EXTENDABLE, AccessWidenerVisitor.AccessType.MUTABLE ->
                            accessTransformer.clazz(className)
                                .merge(AccessTransformer.Visibility.Public, AccessTransformer.FinalModifier.Remove)
                    }

                override fun visitMethod(
                    name: String,
                    descriptor: String,
                    access: AccessWidenerVisitor.AccessType,
                    transitive: Boolean
                ) =
                    when (access) {
                        AccessWidenerVisitor.AccessType.ACCESSIBLE -> {
                            accessTransformer.method(className, name, descriptor)
                                .merge(AccessTransformer.Visibility.Public)
                            accessTransformer.clazz(className)
                                .merge(AccessTransformer.Visibility.Public)
                        }

                        AccessWidenerVisitor.AccessType.EXTENDABLE, AccessWidenerVisitor.AccessType.MUTABLE -> {
                            accessTransformer.method(className, name, descriptor)
                                .merge(AccessTransformer.Visibility.Protected, AccessTransformer.FinalModifier.Remove)
                            accessTransformer.clazz(className)
                                .merge(AccessTransformer.Visibility.Public, AccessTransformer.FinalModifier.Remove)
                        }
                    }

                override fun visitField(
                    name: String,
                    descriptor: String,
                    access: AccessWidenerVisitor.AccessType,
                    transitive: Boolean
                ) =
                    when (access) {
                        AccessWidenerVisitor.AccessType.ACCESSIBLE -> {
                            accessTransformer.field(className, name)
                                .merge(AccessTransformer.Visibility.Public)
                            accessTransformer.clazz(className)
                                .merge(AccessTransformer.Visibility.Public)
                        }

                        AccessWidenerVisitor.AccessType.EXTENDABLE, AccessWidenerVisitor.AccessType.MUTABLE -> {
                            accessTransformer.field(className, name)
                                .merge(AccessTransformer.Visibility.Public, AccessTransformer.FinalModifier.Remove)
                            accessTransformer.clazz(className)
                                .merge(AccessTransformer.Visibility.Public, AccessTransformer.FinalModifier.Remove)
                        }
                    }
            }
    }
}