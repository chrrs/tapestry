package me.chrr.tapestry.gradle

import me.chrr.tapestry.gradle.loader.FabricPlugin
import me.chrr.tapestry.gradle.loader.LoaderPlugin
import me.chrr.tapestry.gradle.loader.NeoForgePlugin
import me.chrr.tapestry.gradle.loader.NeoFormPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.internal.component.DefaultAdhocSoftwareComponent
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

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
            createApiConfigurations(target, plugins, component)
            createMergedJarTask(target, plugins.map { it.target }, tapestry, component)
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
            plugin.applyAfterEvaluate()

        // Apply the loader plugins and depend on the common projects.
        for (plugin in loaderPlugins) {
            plugin.applyAfterEvaluate()
            commonPlugins.forEach { plugin.addBuildDependency(it) }
        }

        return listOf(commonPlugins, loaderPlugins).flatten()
    }

    fun createApiConfigurations(root: Project, plugins: List<LoaderPlugin>, component: AdhocComponentWithVariants) {
        // For each of our platform, we create a separate API configuration. This way, other tapestry
        // projects can depend on different loaders depending on the environment.
        fun createApiConfiguration(name: String, plugins: List<LoaderPlugin>) {
            val api = createProducerConfiguration(
                root, "tapestry${name}ApiElements",
                "tapestry-${name.lowercase()}-api"
            ) {
                plugins.forEach {
                    val jar = it.target.tasks.named<Jar>("jar")
                    outgoing.artifact(jar) {
                        builtBy(jar)
                        classifier = name.lowercase()
                    }

                    it.target.tasks.findByName("sourcesJar")?.let { sources ->
                        outgoing.artifact(sources) {
                            builtBy(sources)
                            classifier = "${name.lowercase()}-sources"
                        }
                    }
                }
            }

            component.addVariantsFromConfiguration(api.get()) {
                mapToMavenScope("compile")
            }
        }

        createApiConfiguration("Common", plugins.filterIsInstance<NeoFormPlugin>())
        createApiConfiguration("Fabric", plugins.filterIsInstance<FabricPlugin>())
        createApiConfiguration("NeoForge", plugins.filterIsInstance<NeoForgePlugin>())
    }

    fun createMergedJarTask(
        root: Project,
        projects: List<Project>,
        tapestry: TapestryExtension,
        component: AdhocComponentWithVariants
    ) {
        // We create two tasks, one for the compiled mod, and one for the sources.
        fun createTask(name: String, sources: Boolean) =
            root.tasks.register<Jar>(name) {
                group = "build"
                description = "Compiles for all loaders and generates a universal mod jar."

                tapestry.applyArchiveName(this, null)
                if (sources) archiveClassifier.set("sources")

                manifest.attributes("Tapestry-Merged-Jar" to "true")
                duplicatesStrategy = DuplicatesStrategy.FAIL

                val jar = projects.map { it.tasks.named("jar") }
                dependsOn(jar)

                val paths = projects
                    .map { it.the<SourceSetContainer>().getByName("main") }
                    .map { if (sources) it.allSource else it.output }
                from(paths)
            }

        val mergedJar = createTask("mergedJar", false)
        val mergedSourcesJar = createTask("mergedSourcesJar", true)
        root.tasks.named("build") { dependsOn(mergedJar) }

        // Then, we register these tasks as the default java-api and java-runtime tasks.
        val api = createProducerConfiguration(root, "tapestryMergedApiElements", Usage.JAVA_API) {
            outgoing.artifact(mergedJar) { builtBy(mergedJar) }
            outgoing.artifact(mergedSourcesJar) { builtBy(mergedSourcesJar) }
        }

        component.addVariantsFromConfiguration(api.get()) {
            mapToMavenScope("compile")
        }

        val runtime = createProducerConfiguration(root, "tapestryMergedRuntimeElements", Usage.JAVA_RUNTIME) {
            outgoing.artifact(mergedJar) { builtBy(mergedJar) }
            outgoing.artifact(mergedSourcesJar) { builtBy(mergedSourcesJar) }
        }

        component.addVariantsFromConfiguration(runtime.get()) {
            mapToMavenScope("runtime")
        }
    }

    private fun createProducerConfiguration(
        target: Project,
        name: String,
        usage: String,
        action: Configuration.() -> Unit
    ): Provider<Configuration> =
        target.configurations.register(name) {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(usage))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, target.objects.named(Category.LIBRARY))
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling.EXTERNAL))

            this.action()

            isCanBeConsumed = true
            isCanBeResolved = false
        }
}