package me.chrr.tapestry.gradle.manifest

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.chrr.tapestry.gradle.GSON
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.model.FabricModJson
import me.chrr.tapestry.gradle.platform.PlatformType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
open class GenerateFabricManifestTask : GenerateManifestTask() {
    @Internal
    val tapestry = project.objects.property<TapestryExtension>()

    @OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    override fun generateManifest(ctx: Context) {
        val manifest = FabricModJson()
        val info = tapestry.get().info
        val transform = tapestry.get().transform
        val depends = tapestry.get().depends

        manifest.id = info.id.get()
        manifest.version = tapestry.get().effectiveVersion.get()

        manifest.name = info.name.orNull
        manifest.description = info.description.orNull
        manifest.environment = info.environment.get().value
        manifest.authors = info.authors.get()
        manifest.contributors = info.contributors.orNull
        manifest.license = info.license.get()
        manifest.icon = info.icon.orNull

        if (info.isLibrary.get() || info.parentMod.isPresent || ctx.implementations.isNotEmpty())
            manifest.custom = JsonObject().apply {
                add("modmenu", JsonObject().apply {
                    if (info.isLibrary.get())
                        add("badges", gsonArray(listOf("library")))
                    info.parentMod.ifPresent { addProperty("parent", it) }
                })

                add("tapestry", JsonObject().apply {
                    add("implementations", JsonObject().apply {
                        for (impl in ctx.implementations)
                            add(impl.key, gsonArray(impl.value))
                    })
                })
            }

        manifest.contact = mutableMapOf<String, String>().also {
            if (info.url.isPresent) {
                it["homepage"] = info.url.get()
                it["sources"] = info.url.get()
            }

            if (info.issues.isPresent) {
                it["issues"] = info.issues.get()
            }
        }

        manifest.entrypoints = ctx.fabricEntrypoints

        manifest.mixins = listOf(transform.mixinConfigs, transform.mixinConfigs(PlatformType.Fabric))
            .flatMap { it.get() }

        manifest.accessWidener = transform.classTweaker.orNull

        val dependencies = mutableMapOf<String, String>()
        manifest.depends = dependencies

        depends.minecraft.ifPresent {
            // FIXME: for now, skip the version dependency if it's not a full release.
            if (it.size == 1 && !it[0].matches(Regex("\\d+(?:\\.\\d+)*")))
                return@ifPresent

            if (it.size == 1)
                dependencies["minecraft"] = it[0]
            else
                dependencies["minecraft"] = ">=${it[0]} <=${it.last()}"
        }

        for (dependency in (depends.fabric.orNull ?: listOf()).filter { !it.optional })
            dependencies[dependency.id] = dependency.version ?: "*"

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(GSON.toJson(manifest))
    }

    private fun gsonArray(list: List<String>): JsonArray {
        val array = JsonArray()
        list.forEach { array.add(it) }
        return array
    }
}