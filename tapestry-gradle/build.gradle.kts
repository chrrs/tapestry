plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "me.chrr.tapestry"
version = gradle.parent!!.rootProject.property("plugin.version") as String

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

gradlePlugin {
    vcsUrl = "https://github.com/chrrs/tapestry"

    plugins.register("me.chrr.tapestry.gradle") {
        id = "me.chrr.tapestry.gradle"
        displayName = "Tapestry Gradle"
        implementationClass = "me.chrr.tapestry.gradle.TapestryPlugin"
        version = project.version as String
    }
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("io.hotmoka:toml4j:0.7.3")

    fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"
    implementation(plugin("net.fabricmc.fabric-loom", "1.15.2"))
    implementation(plugin("net.neoforged.moddev", "2.0.139"))
    implementation(plugin("me.modmuss50.mod-publish-plugin", "1.1.0"))
}

sourceSets {
    val api = register("api")

    main {
        compileClasspath += api.get().output
        runtimeClasspath += api.get().output
    }
}

tasks {
    val apiJar = register<Jar>("apiJar") {
        archiveBaseName = "tapestry-gradle-api"
        archiveClassifier.set("api")
        from(sourceSets.named("api").map { it.output })
    }

    named<Jar>("jar") {
        archiveClassifier.set("")
        into("META-INF/jars") {
            from(apiJar)
            rename { "api.jar" }
        }
    }
}

publishing {
    repositories {
        maven("https://repo.repsy.io/chrrs/releases") {
            name = "ChrrsReleases"
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}