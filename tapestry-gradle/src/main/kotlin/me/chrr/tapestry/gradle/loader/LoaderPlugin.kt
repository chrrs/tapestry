package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.tapestryBuildDir
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class LoaderPlugin(val tapestry: TapestryExtension, val target: Project) {
    abstract fun applyAfterEvaluate()
    abstract fun addBuildDependency(other: LoaderPlugin)

    fun applyJavaPlugin(): JavaPluginExtension {
        target.plugins.apply(JavaLibraryPlugin::class.java)
        val java = target.extensions.getByType<JavaPluginExtension>()
        java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        java.withSourcesJar()

        // Include any generated sources in the final build.
        java.sourceSets.getByName("main").resources
            .srcDir(target.tapestryBuildDir.map { it.dir("generated") })
        target.tasks.getByName("sourcesJar").dependsOn(target.tasks.getByName("processResources"))

        return java
    }

    fun applyAnnotationProcessor() {
        // Depend on the Tapestry Gradle API jar, unpack it if necessary.
        javaClass.classLoader.getResourceAsStream("META-INF/jars/api.jar")?.use {
            val buildDir = target.tapestryBuildDir.get()
            val apiJar = buildDir.file("tapestry-gradle-api.jar")

            buildDir.asFile.mkdirs()
            Files.copy(it, apiJar.asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            target.dependencies.add("annotationProcessor", target.files(apiJar))
            target.dependencies.add("compileOnly", target.files(apiJar))
        } ?: throw IllegalStateException("API jar not found in Tapestry Gradle plugin.")

        // Depend on this plugin as the annotation processor.
        val pluginJar = target.files(javaClass.protectionDomain.codeSource.location)
        target.dependencies.add("annotationProcessor", pluginJar)
    }
}

enum class Loader {
    Fabric,
    NeoForge,
}