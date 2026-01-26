package me.chrr.tapestry.gradle.platform

import me.chrr.tapestry.gradle.PLATFORM_ATTRIBUTE
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.tapestryBuildDir
import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

abstract class Platform(val tapestry: TapestryExtension, val target: Project) {
    abstract val type: PlatformType

    val generatedResourcesDir: DirectoryProperty = target.objects.directoryProperty()
    val sourceSets = target.objects.listProperty<SourceSet>()
    val commonSourceSets = target.objects.listProperty<SourceSet>()

    open fun applyPlatformPlugin() {
        // Apply the Java library plugin to all platform projects.
        target.plugins.apply(JavaLibraryPlugin::class.java)
        val java = target.the<JavaPluginExtension>()

        sourceSets.convention(java.sourceSets.named("main").map { listOf(it) })
        generatedResourcesDir.convention(target.tapestryBuildDir.map { it.dir("generated") })

        val targetJavaVersion = 25
        java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
        java.withSourcesJar()

        // If the project version isn't set, set it to the mod version.
        if (target.version == "unspecified")
            target.version = tapestry.info.version.get()

        // Create a JiJ configuration.
        target.configurations.register("jij") {
            attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, targetJavaVersion)
            attributes.attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage.JAVA_RUNTIME))
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling.EXTERNAL))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, target.objects.named(Category.LIBRARY))

            isTransitive = false
        }

        // Properly set JAR names.
        target.tasks.named<Jar>("jar") { tapestry.applyArchiveName(this, type.name.lowercase()) }
        target.tasks.named<Jar>("sourcesJar") { tapestry.applyArchiveName(this, type.name.lowercase()) }

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

    protected fun preferPlatformAttribute(platform: String) {
        // Prefer configurations with the correct platform attribute.
        abstract class PlatformPreferRule @Inject constructor(val value: String) :
            AttributeDisambiguationRule<String> {
            override fun execute(details: MultipleCandidatesDetails<String>) {
                details.closestMatch(value)
            }
        }

        target.dependencies.attributesSchema.attribute(PLATFORM_ATTRIBUTE) {
            disambiguationRules.add(PlatformPreferRule::class.java) { params(platform) }
        }
    }

    protected fun findResource(name: Provider<String>): Provider<File> = name.flatMap { name ->
        sourceSets.zip(commonSourceSets) { a, b -> a + b }
            .map { sets -> sets.flatMap { it.resources.matching { include(name) } } }
            .map { it.find { file -> file.isFile } }
    }

    protected fun copyGeneratedResources(files: FileCollection) {
        target.tasks.named<ProcessResources>("processResources") {
            val names = files.map { it.relativeTo(generatedResourcesDir.get().asFile).path }

            if (names.isNotEmpty())
                from(generatedResourcesDir) {
                    include(names)
                    duplicatesStrategy = DuplicatesStrategy.FAIL
                }

            dependsOn(files)
        }
    }
}
