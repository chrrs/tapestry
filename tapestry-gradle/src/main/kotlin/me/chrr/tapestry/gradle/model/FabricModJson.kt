package me.chrr.tapestry.gradle.model

import com.google.gson.JsonObject

@Suppress("unused")
class FabricModJson {
    val schemaVersion: Int = 1

    var id: String? = null
    var version: String? = null

    var name: String? = null
    var description: String? = null
    var environment: String? = null
    var authors: List<String> = emptyList()
    var contributors: List<String> = emptyList()
    var license: String? = null
    var icon: String? = null

    var custom: JsonObject? = null
    var contact: Map<String, String> = emptyMap()
    var entrypoints: Map<String, List<String>> = emptyMap()
    var mixins: List<String> = emptyList()

    var accessWidener: String? = null
    var depends: Map<String, String> = emptyMap()
    var jars: List<NestedJar> = emptyList()


    data class NestedJar(val file: String)
}