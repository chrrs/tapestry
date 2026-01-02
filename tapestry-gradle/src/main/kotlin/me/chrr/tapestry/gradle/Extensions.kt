package me.chrr.tapestry.gradle

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

val Project.tapestryBuildDir: Provider<Directory>
    get() = layout.buildDirectory.dir("tapestry")

fun <T : Any> Provider<T>.ifPresent(f: (T) -> Unit) {
    if (isPresent)
        f(get())
}