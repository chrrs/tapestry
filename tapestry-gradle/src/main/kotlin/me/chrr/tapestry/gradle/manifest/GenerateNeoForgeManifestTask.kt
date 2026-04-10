package me.chrr.tapestry.gradle.manifest

import com.moandjiezana.toml.TomlWriter
import me.chrr.tapestry.gradle.TapestryExtension
import me.chrr.tapestry.gradle.ifPresent
import me.chrr.tapestry.gradle.model.NeoForgeModsToml
import me.chrr.tapestry.gradle.platform.PlatformType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
open class GenerateNeoForgeManifestTask : GenerateManifestTask() {
    @Internal
    val tapestry = project.objects.property<TapestryExtension>()

    @OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    override fun generateManifest(ctx: Context) {
        if (ctx.fabricEntrypoints.isNotEmpty())
            throw IllegalStateException("@FabricEntrypoint can only be used for Fabric targets.")

        val manifest = NeoForgeModsToml()
        val info = tapestry.get().info
        val transform = tapestry.get().transform
        val depends = tapestry.get().depends

        manifest.issueTrackerUrl = info.issues.orNull
        manifest.license = info.license.get()

        manifest.mods = listOf(NeoForgeModsToml.Mod().also { mod ->
            mod.modId = info.id.get()
            mod.version = tapestry.get().effectiveVersion.get()

            mod.displayName = info.name.orNull
            mod.description = info.description.orNull
            mod.authors = info.authors.orNull?.joinToString(", ")
            mod.credits = info.contributors.orNull
                ?.let { if (it.isEmpty()) null else it }
                ?.joinToString(", ")
            mod.logoFile = info.banner.orElse(info.icon).orNull
            mod.displayURL = info.url.orNull
        })

        manifest.mixins = listOf(transform.mixinConfigs, transform.mixinConfigs(PlatformType.NeoForge))
            .flatMap { it.get() }
            .map { NeoForgeModsToml.Mixin(it) }

        val dependencies = mutableListOf<NeoForgeModsToml.Dependency>()
        manifest.dependencies = mapOf(info.id.get() to dependencies)

        depends.minecraft.ifPresent {
            if (it.size == 1)
                dependencies.add(NeoForgeModsToml.Dependency("minecraft", "[${it[0]}]"))
            else
                dependencies.add(NeoForgeModsToml.Dependency("minecraft", "[${it[0]}, ${it.last()}]"))
        }

        for (dependency in (depends.neoforge.orNull ?: listOf()).filter { !it.optional })
            dependencies.add(NeoForgeModsToml.Dependency(dependency.id, dependency.version ?: "(,)"))

        manifest.modproperties = mapOf(
            info.id.get() to mapOf<String, Any>(
                "tapestry" to mapOf<String, Any>(
                    "implementations" to ctx.implementations
                )
            )
        )

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(TomlWriter().write(manifest))
    }
}