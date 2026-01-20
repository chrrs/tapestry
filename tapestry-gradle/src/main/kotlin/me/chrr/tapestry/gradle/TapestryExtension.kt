package me.chrr.tapestry.gradle

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import java.io.File
import javax.inject.Inject

abstract class TapestryExtension(objects: ObjectFactory) {
    val versions = objects.newInstance<Versions>(objects)
    val projects = objects.newInstance<Projects>(objects)
    val info = objects.newInstance<Info>(objects)
    val transform = objects.newInstance<Transform>(objects)
    val depends = objects.newInstance<Depends>(objects)
    val game = objects.newInstance<Game>(objects)
    val publish = objects.newInstance<Publish>(info, objects)

    fun projects(f: Projects.() -> Unit) = projects.apply(f)
    fun versions(f: Versions.() -> Unit) = versions.apply(f)
    fun info(f: Info.() -> Unit) = info.apply(f)
    fun transform(f: Transform.() -> Unit) = transform.apply(f)
    fun depends(f: Depends.() -> Unit) = depends.apply(f)
    fun game(f: Game.() -> Unit) = game.apply(f)
    fun publish(f: Publish.() -> Unit) = publish.apply(f)

    fun Project.prop(name: String): Provider<String> =
        project.providers.gradleProperty(name)

    internal fun isCI() = System.getenv("CI") != null
    internal fun isRelease() = System.getenv("RELEASE") != null

    internal fun applyArchiveName(task: AbstractArchiveTask, appendix: String?) {
        val version = info.version.get() + "+mc" + versions.minecraft.get()
        task.archiveBaseName.set(info.id)
        appendix?.let { task.archiveAppendix.set(it) }
        task.archiveVersion.set(version)
    }

    open class Versions @Inject constructor(objects: ObjectFactory) {
        val minecraft = objects.property<String>()
        val neoform = objects.property<String>()
        val fabricLoader = objects.property<String>()
        val neoforge = objects.property<String>()
    }

    open class Projects @Inject constructor(objects: ObjectFactory) {
        val common: ListProperty<Project> = objects.listProperty<Project>().unsetConvention()
        val fabric: ListProperty<Project> = objects.listProperty<Project>().unsetConvention()
        val neoforge: ListProperty<Project> = objects.listProperty<Project>().unsetConvention()
    }

    open class Info @Inject constructor(objects: ObjectFactory) {
        val id = objects.property<String>()
        val version = objects.property<String>()

        val name = objects.property<String>()
        val description = objects.property<String>()
        val environment = objects.property<Environment>().apply { convention(Environment.Both) }
        val authors = objects.listProperty<String>().apply { convention(emptyList()) }
        val contributors = objects.listProperty<String>()
        val license = objects.property<String>().apply { convention("ARR") }

        val url = objects.property<String>()
        val sources = objects.property<String>()
        val issues = objects.property<String>()

        val icon = objects.property<String>()
        val parentMod = objects.property<String>()
        val isLibrary = objects.property<Boolean>().apply { convention(false) }
    }

    open class Transform @Inject constructor(objects: ObjectFactory) {
        val classTweakers: ListProperty<String> = objects.listProperty<String>()
        val mixinConfigs: ListProperty<String> = objects.listProperty<String>()
    }

    open class Depends @Inject constructor(objects: ObjectFactory) {
        // FIXME: use this.
        val minecraft = objects.listProperty<String>()
    }

    open class Game @Inject constructor(objects: ObjectFactory) {
        val runDir: DirectoryProperty = objects.directoryProperty()
        val username = objects.property<String>()
    }

    open class Publish @Inject constructor(private val info: Info, objects: ObjectFactory) {
        val changelog = objects.property<String>()
        val mergedJar = objects.property<Boolean>()
        val modrinth = objects.property<String>()
        val curseforge = objects.property<String>()

        fun readChangelogFrom(file: File) {
            // Find the Markdown changelog section for the current version.
            changelog.set(info.version.map { version ->
                var changes = file.readText()

                val versionHeader = Regex($$"""(?:^|\n)\s*##\s*$${Regex.escape(version)}\s*(?:$|\n)""")
                val anyHeader = Regex("""(?:^|\n)\s*##.*(?:$|\n)""")

                val start = versionHeader.find(changes, 0)?.range?.last
                    ?: throw IllegalArgumentException("${file.name} does not contain a changelog entry for version $version")
                changes = changes.substring(start + 1)

                anyHeader.find(changes)?.let {
                    changes = changes.substring(0, it.range.last + 1)
                }

                return@map changes.trim()
            })
        }
    }
}