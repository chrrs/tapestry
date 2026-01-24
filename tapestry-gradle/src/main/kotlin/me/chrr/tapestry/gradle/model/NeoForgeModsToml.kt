package me.chrr.tapestry.gradle.model

@Suppress("unused")
class NeoForgeModsToml {
    val modLoader: String = "javafml"
    val loaderVersion: String = "[1,)"

    var issueTrackerUrl: String? = null
    var license: String? = null

    var mods: List<Mod> = emptyList()
    var mixins: List<Mixin> = emptyList()
    var dependencies: Map<String, List<Dependency>> = emptyMap()
    var modproperties: Map<String, Any> = emptyMap()


    class Mod {
        var modId: String? = null
        var version: String? = null

        var displayName: String? = null
        var description: String? = null
        var authors: String? = null
        var credits: String? = null
        var logoFile: String? = null
        val logoBlur: Boolean = false
        var displayURL: String? = null
    }

    data class Dependency(val modId: String, val versionRange: String)
    data class Mixin(val config: String)
}