package me.chrr.tapestry.gradle.platform

import me.chrr.tapestry.gradle.TapestryExtension
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project

abstract class LoaderPlatform(tapestry: TapestryExtension, target: Project) : Platform(tapestry, target) {
    val commonPlatforms = target.objects.listProperty<CommonPlatform>()
    abstract val jijConfigurationName: String
    abstract val compatibleLoaders: List<String>

    override fun applyPlatformPlugin() {
        super.applyPlatformPlugin()

        target.afterEvaluate {
            target.configurations.named(jijConfigurationName) {
                // Copy all the JiJ dependencies from common projects to this project.
                dependencies.addAllLater(commonPlatforms.map { platforms ->
                    platforms.flatMap { it.target.configurations.getByName("jij").incoming.dependencies }
                })

                // Copy all the JiJ dependencies from this project to the target jij configuration.
                dependencies.addAllLater(project.configurations.named("jij").map { it.incoming.dependencies })
            }

            // Copy all the repositories of the other platform to this project.
            target.repositories.addAll(commonPlatforms.get().flatMap { it.target.repositories.toList() })
        }

        // Add all source sets of the other platform to this project.
        target.tasks.named<Jar>("jar") { from(commonSourceSets.map { set -> set.map { it.output } }) }
        target.tasks.named<Jar>("sourcesJar") { from(commonSourceSets.map { set -> set.map { it.allSource } }) }
        commonSourceSets.addAll(commonPlatforms.map { platforms -> platforms.flatMap { it.sourceSets.get() } })

        target.configurations.named("implementation") {
            dependencies.addAllLater(commonPlatforms.map { platforms ->
                platforms.map { target.dependencies.project(it.target.path) }
            })
        }
    }
}