package me.chrr.tapestry.gradle.manifest

import me.chrr.tapestry.gradle.TapestryExtension

data class ManifestContext(
    val tapestry: TapestryExtension,
    val fabricEntrypoints: Map<String, List<String>>,
    val platformImplementations: Map<String, List<String>>
)
