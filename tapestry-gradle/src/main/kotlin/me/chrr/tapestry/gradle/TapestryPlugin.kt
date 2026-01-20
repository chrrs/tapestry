package me.chrr.tapestry.gradle

import me.chrr.tapestry.gradle.loader.FabricPlugin
import me.chrr.tapestry.gradle.loader.LoaderPlugin
import me.chrr.tapestry.gradle.loader.NeoForgePlugin
import me.chrr.tapestry.gradle.loader.NeoFormPlugin
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

class TapestryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val tapestry = target.extensions.create<TapestryExtension>("tapestry")

        // fixme: move '@PlatformImplementation' to tapestry-base?
        // fixme: platforms should use some kind of loader-specific services?
        //        -> use reflection to detect presence of loader classes.
        // idea: some kind of '@Initializer' annotation that calls a static method
        // idea: an 'updateVersions' task that auto-updates build.gradle.kts

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

            // We apply only the java-base plugin, since we don't have any source code in the root project by default.
            // We then create a new "tapestry" configuration to house all of our merged JAR configurations.
            // FIXME: DefaultAdhocSoftwareComponent is an internal API.
            target.plugins.apply(JavaBasePlugin::class.java)
            val component = DefaultAdhocSoftwareComponent("tapestry", target.objects)
            target.components.add(component)

            // Finally, we process all the loader subprojects.
            val plugins = applyLoaderPlugins(target, tapestry)
            createMergedJarTask(target, plugins, tapestry)
            createConfigurations(target, plugins, component)
        }
    }

    fun applyLoaderPlugins(target: Project, tapestry: TapestryExtension): List<LoaderPlugin> {
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

        // Create NeoForm plugin for all common projects.
        val common = tapestry.projects.common.getPresentOr { listOf(target.project("common")) }
        val commonPlugins = common.map { NeoFormPlugin(tapestry, it) }

        // Create loader-specific plugin for all loader projects.
        val loaderPlugins = mutableListOf<LoaderPlugin>()

        val fabric = tapestry.projects.fabric.getPresentOr { listOf(target.project("fabric")) }
        if (tapestry.versions.fabricLoader.isPresent)
            loaderPlugins.addAll(fabric.map { FabricPlugin(tapestry, it) })

        val neoforge = tapestry.projects.neoforge.getPresentOr { listOf(target.project("neoforge")) }
        if (tapestry.versions.neoforge.isPresent)
            loaderPlugins.addAll(neoforge.map { NeoForgePlugin(tapestry, it) })

        // Apply the common plugins.
        for (plugin in commonPlugins)
            plugin.applyLoaderPlugin()

        // Apply the loader plugins and depend on the common projects.
        for (plugin in loaderPlugins) {
            plugin.applyLoaderPlugin()
            commonPlugins.forEach { plugin.addBuildDependency(it) }
        }

        return listOf(commonPlugins, loaderPlugins).flatten()
    }

    fun createConfigurations(root: Project, plugins: List<LoaderPlugin>, component: AdhocComponentWithVariants) {
        fun createProducerConfiguration(name: String, usage: String, configure: Configuration.() -> Unit) =
            root.configurations.register(name) {
                attributes.attribute(Usage.USAGE_ATTRIBUTE, root.objects.named(usage))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, root.objects.named(Category.LIBRARY))
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, root.objects.named(Bundling.EXTERNAL))

                configure.invoke(this)

                isCanBeConsumed = true
                isCanBeResolved = false
            }

        // Create a compile-time configuration that defaults to the merged jar.
        val apiElements = createProducerConfiguration("tapestryApiElements", Usage.JAVA_API) {
            outgoing.artifact(root.tasks.named("mergedJar"))

            component.addVariantsFromConfiguration(this) {
                mapToMavenScope("compile")
            }
        }

        val sourcesElements = createProducerConfiguration("tapestrySourcesElements", Usage.JAVA_RUNTIME) {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, root.objects.named(Category.DOCUMENTATION))
            attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, root.objects.named(DocsType.SOURCES))

            outgoing.artifact(root.tasks.named("mergedSourcesJar"))

            component.addVariantsFromConfiguration(this) {
                mapToMavenScope("runtime")
            }
        }

        // Then, for each platform, we create a variant with an attribute.
        fun addVariant(name: String, loaderPlugins: List<LoaderPlugin>) {
            // FIXME: this breaks with multiple loader plugins.
            apiElements.configure {
                outgoing.variants.create(name) {
                    attributes.attribute(PLATFORM_ATTRIBUTE, name.lowercase())

                    loaderPlugins
                        .mapNotNull { it.target.tasks.findByName("jar") }
                        .forEach {
                            artifact(it) { classifier = name.lowercase() }
                        }
                }
            }

            sourcesElements.configure {
                outgoing.variants.create(name) {
                    attributes.attribute(PLATFORM_ATTRIBUTE, name.lowercase())

                    loaderPlugins
                        .mapNotNull { it.target.tasks.findByName("sourcesJar") }
                        .forEach {
                            artifact(it) { classifier = "${name.lowercase()}-sources" }
                        }
                }
            }
        }

        addVariant("Common", plugins.filterIsInstance<NeoFormPlugin>())
        addVariant("Fabric", plugins.filterIsInstance<FabricPlugin>())
        addVariant("NeoForge", plugins.filterIsInstance<NeoForgePlugin>())

        // At runtime, we can just use the merged jar always.
        createProducerConfiguration("tapestryRuntimeElements", Usage.JAVA_RUNTIME) {
            outgoing.artifact(root.tasks.named("mergedJar"))

            component.addVariantsFromConfiguration(this) {
                mapToMavenScope("runtime")
            }
        }

    }

    fun createMergedJarTask(
        root: Project,
        plugins: List<LoaderPlugin>,
        tapestry: TapestryExtension,
    ) {
        fun createTask(name: String, sources: Boolean) =
            root.tasks.register<Jar>(name) {
                group = "build"
                description = "Compiles for all loaders and generates a universal mod jar."

                tapestry.applyArchiveName(this, null)
                if (sources) archiveClassifier.set("sources")

                manifest.attributes("Tapestry-Merged-Jar" to "true")
                duplicatesStrategy = DuplicatesStrategy.FAIL

                val jar = plugins.map { it.target.tasks.named("jar") }
                dependsOn(jar)

                val paths = plugins
                    .flatMap { it.sourceSets }
                    .map { set -> set.map { if (sources) it.allSource else it.output } }
                from(paths)
            }

        // Create a task for both the final JAR, and the sources JAR.
        val mergedJar = createTask("mergedJar", false)
        root.tasks.named("build") { dependsOn(mergedJar) }

        createTask("mergedSourcesJar", true)
    }
}