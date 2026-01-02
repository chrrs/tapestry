package me.chrr.tapestry.gradle.manifest

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import me.chrr.tapestry.gradle.ifPresent

object FabricManifest {
    fun generate(ctx: ManifestContext): String {
        val info = ctx.tapestry.info
        val json = JsonObject().apply {
            addProperty("schemaVersion", 1)

            addProperty("id", info.id.get())
            addProperty("version", info.version.get())

            info.name.ifPresent { addProperty("name", it) }
            info.description.ifPresent { addProperty("description", it) }
            addProperty("environment", info.environment.get().value)
            add("authors", gsonArray(info.authors.get()))
            info.contributors.ifPresent { add("contributors", gsonArray(it)) }
            addProperty("license", info.license.get())

            info.icon.ifPresent { addProperty("icon", it) }

            add("custom", JsonObject().apply {
                if (info.isLibrary.get() || info.parentMod.isPresent)
                    add("modmenu", JsonObject().apply {
                        if (info.isLibrary.get())
                            add("badges", gsonArray(listOf("library")))
                        info.parentMod.ifPresent { addProperty("parent", it) }
                    })

                if (ctx.platformImplementations.isNotEmpty())
                    add("tapestry", JsonObject().apply {
                        add("platformImplementations", JsonArray().apply {
                            ctx.platformImplementations
                                .flatMap { it.value.map { value -> it.key to value } }
                                .forEach { (impl, clazz) ->
                                    add(JsonObject().apply {
                                        addProperty("class", clazz)
                                        addProperty("implements", impl)
                                    })
                                }
                        })
                    })
            })

            if (info.url.isPresent || info.sources.isPresent || info.issues.isPresent)
                add("contact", JsonObject().apply {
                    info.url.ifPresent { addProperty("homepage", it) }
                    info.sources.ifPresent { addProperty("sources", it) }
                    info.issues.ifPresent { addProperty("issues", it) }
                })

            if (ctx.fabricEntrypoints.isNotEmpty())
                add("entrypoints", JsonObject().apply {
                    for ((key, values) in ctx.fabricEntrypoints.entries) {
                        add(key, gsonArray(values))
                    }
                })

            // FIXME: mixins
            // FIXME: accesswidener

            add("depends", JsonObject().apply {
                // FIXME: set the minecraft dependency properly.
                addProperty("minecraft", "*")

                // FIXME: support custom dependencies.
            })
        }

        return Gson().toJson(json)
    }

    private fun gsonArray(list: List<String>): JsonArray {
        val array = JsonArray()
        list.forEach { array.add(it) }
        return array
    }
}