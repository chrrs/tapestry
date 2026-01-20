package me.chrr.tapestry.gradle.manifest

import com.moandjiezana.toml.TomlWriter
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.property

open class GenerateNeoForgeManifestTask : GenerateManifestTask() {
    @Internal
    val info = project.objects.property<TapestryExtension.Info>()

    @OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    override fun generateManifest(ctx: Context) {
        if (ctx.fabricEntrypoints.isNotEmpty())
            throw IllegalStateException("@FabricEntrypoint can only be used for Fabric targets.")

        val info = info.get()
        val toml = mutableMapOf<String, Any>().apply {
            this["modLoader"] = "javafml"
            this["loaderVersion"] = "[1,)"

            info.issues.ifPresent { this["issueTrackerUrl"] = it }
            this["license"] = info.license.get()

            this["mods"] = listOf(mutableMapOf<String, Any>().apply {
                this["modId"] = info.id.get()
                this["version"] = info.version.get()

                info.name.ifPresent { this["displayName"] = it }
                info.description.ifPresent { this["description"] = it }

                this["authors"] = info.authors.get().joinToString(", ")
                info.contributors.ifPresent {
                    if (it.isNotEmpty())
                        this["credits"] = it.joinToString(", ")
                }

                info.icon.ifPresent { this["logoFile"] = it }

                this["logoBlur"] = false
                info.url.ifPresent { this["displayURL"] = it }

                // FIXME: mixins
                // FIXME: accesstransformers
            })

            this["dependencies"] = mapOf(info.id.get() to mutableListOf<Any>().apply {
                // FIXME: set the minecraft dependency properly.
                add(mutableMapOf<String, Any>().apply {
                    this["modId"] = "minecraft"
                    this["versionRange"] = "[1,)"
                })

                // FIXME: support custom dependencies.
            })

            if (ctx.platformImplementations.isNotEmpty())
                this["modproperties"] = mapOf(
                    info.id.get() to mapOf<String, Any>(
                        "tapestry" to mapOf<String, Any>(
                            "platformImplementations" to ctx.platformImplementations
                                .flatMap { it.value.map { value -> it.key to value } }
                                .map { (impl, clazz) ->
                                    mapOf<String, Any>(
                                        "class" to clazz,
                                        "implements" to impl,
                                    )
                                }
                        )
                    )
                )
        }

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(TomlWriter().write(toml))
    }
}