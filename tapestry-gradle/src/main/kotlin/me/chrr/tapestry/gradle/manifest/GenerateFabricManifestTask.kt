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

        dependencies["minecraft"] = depends.minecraft.version.toFabricVersionString()
        for (dependency in depends.dependencies.get().filterNot { it.optional }) {
            val modId = dependency.fabric ?: continue
            dependencies[modId] = dependency.version.toFabricVersionString()
        }

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