package me.chrr.tapestry.gradle.jij

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.chrr.tapestry.gradle.GSON
import me.chrr.tapestry.gradle.PLATFORM_ATTRIBUTE
import me.chrr.tapestry.gradle.loader.Platform
import net.neoforged.moddev.shadow.net.neoforged.jarjar.metadata.*
import net.neoforged.moddev.shadow.org.apache.maven.artifact.versioning.DefaultArtifactVersion
import net.neoforged.moddev.shadow.org.apache.maven.artifact.versioning.VersionRange
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

open class PrepareJiJJarsTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.NAME_ONLY)
    val inputFiles: ConfigurableFileCollection = project.objects.fileCollection()
    private val artifacts = project.objects.listProperty<DependencyArtifact>()

    @Input
    @Suppress("unused")
    val dependencyIds: Provider<List<String>> = artifacts.map { artifacts ->
        // InputFiles as a cache check is not enough when multiple platforms can JiJ
        // the same dependency. So we need another unique identifier per platform.
        artifacts.map { it.fileName }
    }

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    fun configuration(platform: Platform, configuration: Provider<Configuration>) {
        dependsOn(configuration)

        val view = configuration.map { configuration ->
            configuration.incoming.artifactView {
                attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)

                // If we're on the common platform, clear the platform attribute so we get the merged JAR.
                if (platform == Platform.Common) attributes.attribute(PLATFORM_ATTRIBUTE, "")
            }
        }

        inputFiles.from(view.map { view -> view.files })
        artifacts.addAll(view.map { view -> view.artifacts.map { DependencyArtifact(platform, it) } })
    }

    @TaskAction
    fun prepare() {
        val outputDir = outputDir.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val artifacts = artifacts.get()
        if (artifacts.isEmpty())
            project.logger.error("JiJ has input files, but no artifacts.")

        for (artifact in artifacts) {
            if (artifact.platform == Platform.Fabric || artifact.platform == Platform.Common) {
                val outputFile = outputDir.resolve(artifact.fileName)
                artifact.file.copyTo(outputFile, true)

                // If the artifact is included on the fabric side, we might need to generate
                // a fabric.mod.json file.
                openZip(outputFile).use {
                    val path = it.getPath("fabric.mod.json")
                    if (!path.exists())
                        path.writeText(GSON.toJson(artifact.generateFabricModJson()))
                }
            } else {
                // By default, just copy over the JAR file.
                project.copy {
                    from(artifact.file)
                    into(outputDir)
                    rename { artifact.fileName }
                }
            }

        }
    }

    fun writeManifests(jar: File) {
        openZip(jar).use { jar ->
            // Merge a "jars" section into fabric.mod.json if necessary.
            val fabricArtifacts = artifacts.get()
                .filter { it.platform == Platform.Fabric || it.platform == Platform.Common }
            if (fabricArtifacts.isNotEmpty()) {
                val fabricModJson = jar.getPath("fabric.mod.json")
                val contents = fabricModJson.readText()

                val root = GSON.fromJson(contents, JsonObject::class.java)
                root.add("jars", JsonArray().apply {
                    for (artifact in fabricArtifacts) {
                        add(JsonObject().apply {
                            addProperty("file", "META-INF/jars/${artifact.fileName}")
                        })
                    }
                })

                fabricModJson.writeText(GSON.toJson(root))
            }

            // Create a META-INF/jarjar/manifest.json file if necessary.
            val neoforgeArtifacts = artifacts.get()
                .filter { it.platform == Platform.NeoForge || it.platform == Platform.Common }
            if (neoforgeArtifacts.isNotEmpty()) {
                val metadata = Metadata(neoforgeArtifacts.map {
                    ContainedJarMetadata(
                        ContainedJarIdentifier(it.module.group, it.module.name),
                        ContainedVersion(
                            VersionRange.createFromVersionSpec("[${it.module.version},)"),
                            DefaultArtifactVersion(it.module.version)
                        ),
                        "META-INF/jars/${it.fileName}", false
                    )
                })

                val metadataJson = jar.getPath("META-INF/jarjar/metadata.json")
                metadataJson.parent.createDirectories()
                metadataJson.writeText(MetadataIOHandler.toLines(metadata).joinToString("\n"))
            }
        }
    }

    private fun openZip(file: File) =
        FileSystems.newFileSystem(URI("jar", "${file.toURI()}", null), mapOf("create" to "false"))
}