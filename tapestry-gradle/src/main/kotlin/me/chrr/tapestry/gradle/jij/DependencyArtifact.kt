package me.chrr.tapestry.gradle.jij

import com.google.gson.JsonObject
import me.chrr.tapestry.gradle.loader.Platform
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

class DependencyArtifact {
    val platform: Platform
    val file: File

    val module: Identifier
    val classifier: String?

    val fileName
        get() = "${platform.name.lowercase()}_${file.name}"

    constructor(platform: Platform, artifact: ResolvedArtifactResult) {
        this.platform = platform
        this.file = artifact.file

        var module: Identifier? = null
        val id = artifact.variant.owner
        if (id is ModuleComponentIdentifier) {
            module = Identifier(id.group, id.module, id.version)
        }

        val capabilities = artifact.variant.capabilities
            .map { Identifier(it.group, it.name, it.version) }
        if (module != null && capabilities.contains(module)) {
            this.module = module
        } else {
            this.module = capabilities.firstOrNull()
                ?: throw IllegalArgumentException("JiJ artifact $id is not a module component and has no capabilities")
        }

        val prefix = "${this.module.name}-${this.module.version}"
        if (this.file.name.startsWith(prefix)) {
            this.classifier = this.file.name.substring(prefix.length)
                .substringBefore('.')
        } else {
            this.classifier = null
        }
    }

    fun generateFabricModJson() =
        JsonObject().apply {
            val modId = "${module.group}_${module.name}${classifier?.let { "_$it" } ?: ""}"
                .replace('.', '_').lowercase()

            addProperty("schemaVersion", 1)
            addProperty("id", modId)
            addProperty("version", module.version ?: "unspecified")
            addProperty("name", module.name)
            add("custom", JsonObject().apply {
                addProperty("fabric-loom:generated", true)
                addProperty("tapestry:generated", true)
            })
        }

    data class Identifier(val group: String, val name: String, val version: String?)
}