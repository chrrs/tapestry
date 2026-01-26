package me.chrr.tapestry.gradle

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.chrr.tapestry.gradle.jij.PrepareJiJJarsTask
import me.chrr.tapestry.gradle.platform.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.internal.component.DefaultAdhocSoftwareComponent
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

val PLATFORM_ATTRIBUTE: Attribute<String> = Attribute.of("me.chrr.tapestry.platform", String::class.java)
val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

@Suppress("unused")
class TapestryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val tapestry = target.extensions.create<TapestryExtension>("tapestry")

        // We apply only the java-base plugin, since we don't have any source code in the root project by default.
        // We then create a new "tapestry" configuration to house all of our merged JAR configurations.
        // FIXME: DefaultAdhocSoftwareComponent is an internal API.
        target.plugins.apply(JavaBasePlugin::class.java)
        val component = DefaultAdhocSoftwareComponent("tapestry", target.objects)
        target.components.add(component)

        target.afterEvaluate {
            // Set a few of the options to values depending on the environment.
            val versionTag = when {
                tapestry.isRelease() -> ""
                tapestry.isCI() -> "-snapshot"
                else -> "-local"
            }
            tapestry.info.version.set("${tapestry.info.version.get()}$versionTag")

            if (!tapestry.game.runDir.isPresent)
                tapestry.game.runDir.set(target.projectDir.resolve("run"))

            // If the project version isn't set, set it to the mod version.
            if (target.version == "unspecified")
                target.version = tapestry.info.version.get()

            // Process all the loader subprojects.
            val platforms = registerPlatforms(target, tapestry)
            createMergedJarTask(target, platforms, tapestry)
            createConfigurations(target, platforms, component)
        }
    }

    fun registerPlatforms(target: Project, tapestry: TapestryExtension): List<Platform> {
        // Make sure there are no duplicate projects.
        val allProjects = listOfNotNull(
            tapestry.projects.common.orNull,
            tapestry.projects.neoforge.orNull,
            tapestry.projects.fabric.orNull
        ).flatten()
        val duplicates = allProjects.groupingBy { it }.eachCount().filter { it.value > 1 }.map { it.key.name }

        if (duplicates.isNotEmpty())
            throw IllegalArgumentException("Project(s) $duplicates are defined more than once.")
        if (allProjects.contains(target))
            throw IllegalArgumentException("The root project cannot be a loader project.")

        // Create platforms for all projects.
        val platforms = listOf(
            tapestry.projects.common.getPresentOr { listOf(target.project("common")) }
                .map { CommonPlatform(tapestry, it) },
            tapestry.projects.common.getPresentOr { listOf(target.project("fabric")) }
                .map { FabricPlatform(tapestry, it) },
            tapestry.projects.common.getPresentOr { listOf(target.project("neoforge")) }
                .map { NeoForgePlatform(tapestry, it) },
        ).flatten()

        // Register the common platforms with all the loader platforms.
        val loaders = platforms.mapNotNull { it as? LoaderPlatform }
        val commons = platforms.mapNotNull { it as? CommonPlatform }

        for (platform in commons)
            loaders.forEach { it.commonPlatforms.add(platform) }

        // Apply all the platform plugins.
        for (platform in platforms)
            platform.applyPlatformPlugin()

        return platforms
    }

    fun createConfigurations(root: Project, platforms: List<Platform>, component: AdhocComponentWithVariants) {
        fun createProducerConfiguration(name: String, usage: String, configure: Configuration.() -> Unit) =
            root.configurations.register(name) {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, root.objects.named(usage))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, root.objects.named(Category.LIBRARY))
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, root.objects.named(Bundling.EXTERNAL))

                isCanBeConsumed = true
                isCanBeResolved = false

                configure.invoke(this)
            }

        // Create producer configurations for API elements, sources and runtime.
        val apiElements = createProducerConfiguration("tapestryApiElements", Usage.JAVA_API) {
            outgoing.artifact(root.tasks.named("mergedJar"))
        }

        val runtimeElements = createProducerConfiguration("tapestryRuntimeElements", Usage.JAVA_RUNTIME) {
            outgoing.artifact(root.tasks.named("mergedJar"))
        }

        val sourcesElements = createProducerConfiguration("tapestrySourcesElements", Usage.JAVA_RUNTIME) {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, root.objects.named(Category.DOCUMENTATION))
            attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, root.objects.named(DocsType.SOURCES))
            outgoing.artifact(root.tasks.named("mergedSourcesJar"))
        }

        // Then, for each platform, we create a variant with a platform attribute.
        fun addVariant(type: PlatformType, platforms: List<Platform>) {
            fun Configuration.addArtifacts(task: String, suffix: String = "") {
                outgoing.variants.register(type.name) {
                    attributes.attribute(PLATFORM_ATTRIBUTE, type.name.lowercase())

                    platforms
                        .mapNotNull { it.target.tasks.findByName(task) }
                        .forEach {
                            artifact(it) { classifier = it.name.lowercase() + suffix }
                        }
                }
            }

            apiElements.configure { addArtifacts("jar") }
            runtimeElements.configure { addArtifacts("jar") }
            sourcesElements.configure { addArtifacts("sourcesJar", "-sources") }
        }

        for ((type, platforms) in platforms.groupBy { it.type })
            addVariant(type, platforms)

        // Finally, add them all to the component.
        component.addVariantsFromConfiguration(apiElements.get()) { mapToMavenScope("compile") }
        component.addVariantsFromConfiguration(sourcesElements.get()) { }
        component.addVariantsFromConfiguration(runtimeElements.get()) { mapToMavenScope("runtime") }
    }

    fun createMergedJarTask(root: Project, platforms: List<Platform>, tapestry: TapestryExtension) {
        // Create the task to prepare the JiJ jars.
        val prepare = root.tasks.register<PrepareJiJJarsTask>("prepareJijJars") {
            for (platform in platforms)
                configuration(platform.type, platform.target.configurations.named("jij"))
            outputDir.set(root.tapestryBuildDir.map { it.dir("jijJars") })
        }

        // Create the task for both the final JAR, and the sources JAR.
        fun createTask(name: String, sources: Boolean) =
            root.tasks.register<Jar>(name) {
                group = "build"
                description = "Compiles for all loaders and generates a universal mod jar."

                tapestry.applyArchiveName(this, null)
                if (sources) archiveClassifier.set("sources")

                manifest.attributes(
                    "Tapestry-Merged-Jar" to "true",
                    "Fabric-Mapping-Namespace" to "official",
                    "Fabric-Minecraft-Version" to tapestry.versions.minecraft.get(),
                    "Fabric-Loader-Version" to tapestry.versions.fabricLoader.get(),
                )

                duplicatesStrategy = DuplicatesStrategy.FAIL

                val paths = platforms
                    .map { it.sourceSets.map { sets -> sets.map { set -> if (sources) set.allSource else set.output } } }
                from(paths)

                if (!sources) {
                    from(prepare) { into("META-INF/jars") }

                    // FIXME: I feel like there should be a way to do this more idiomatic.
                    dependsOn(prepare)
                    doLast { prepare.get().writeManifests(archiveFile.get().asFile) }
                }
            }

        val mergedJar = createTask("mergedJar", false)
        root.tasks.named("build") { dependsOn(mergedJar) }

        createTask("mergedSourcesJar", true)
    }
}