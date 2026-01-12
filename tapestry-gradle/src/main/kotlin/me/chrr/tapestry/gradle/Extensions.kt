package me.chrr.tapestry.gradle

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

val Project.tapestryBuildDir: Provider<Directory>
    get() = layout.buildDirectory.dir("tapestry")

fun <T : Any, V> Provider<T>.ifPresent(f: (T) -> V) =
    if (isPresent) f(get()) else null

fun <T : Any> Provider<T>.getPresentOr(other: () -> T): T =
    if (isPresent) get() else other()