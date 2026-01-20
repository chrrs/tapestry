package me.chrr.tapestry.gradle.loader

import me.chrr.tapestry.gradle.PLATFORM_ATTRIBUTE
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.classtweaker.ConvertClassTweakersTask
import me.chrr.tapestry.gradle.tapestryBuildDir
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import javax.inject.Inject

abstract class LoaderPlugin(val tapestry: TapestryExtension, val target: Project) {
    val generatedResourcesDir: Provider<Directory>
            by lazy { target.tapestryBuildDir.map { it.dir("generated") } }

    val ownSourceSets: List<Provider<SourceSet>>
            by lazy { listOf(target.the<SourceSetContainer>().named("main")) }
    val otherSourceSets = mutableListOf<Provider<SourceSet>>()

    abstract fun applyLoaderPlugin()
    abstract fun addBuildDependency(other: LoaderPlugin)

    fun applyJavaPlugin(appendix: String): JavaPluginExtension {
        target.plugins.apply(JavaLibraryPlugin::class.java)
        val java = target.the<JavaPluginExtension>()
        java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        java.withSourcesJar()

        // Properly set JAR names.
        target.tasks.named<Jar>("jar") { tapestry.applyArchiveName(this, appendix) }
        target.tasks.named<Jar>("sourcesJar") { tapestry.applyArchiveName(this, appendix) }

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

    fun preferPlatformAttribute(platform: String) {
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

        // We warn in case we still somehow are including a merged jar dependency.
        target.configurations.configureEach {
            if (!isCanBeResolved || !name.contains("compile"))
                return@configureEach

            incoming.afterResolve {
                resolvedConfiguration.firstLevelModuleDependencies
                    .flatMap { it.allModuleArtifacts.map { artifact -> it to artifact } }
                    .filter { it.second.extension == "jar" }
                    .forEach { (dependency, artifact) ->
                        JarFile(artifact.file).use {
                            val attributes = it.manifest?.mainAttributes ?: return@use
                            if (attributes.getValue("Tapestry-Merged-Jar") == "true") {
                                target.logger.error("Dependency '${dependency.name}' in project '${target.path}' is a Tapestry merged mod JAR.")
                            }
                        }
                    }
            }
        }
    }

    fun createAccessTransformerTask() =
        target.tasks.register<ConvertClassTweakersTask>("convertClassTweaker") {
            if (tapestry.transform.classTweaker.isPresent) {
                inputFiles.add(findResource(tapestry.transform.classTweaker))
                outputFiles.add(generatedResourcesDir.map { it.file("META-INF/accesstransformer.cfg").asFile })
            }
        }

    fun findResource(name: Provider<String>): Provider<File> = name.map { name ->
        listOf(ownSourceSets, otherSourceSets).flatten().map { it.get() }
            .flatMap { it.resources.matching { include(name) } }
            .find { it.isFile }
    }

    fun copyGeneratedResources(files: FileCollection) {
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
