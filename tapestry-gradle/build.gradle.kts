plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "me.chrr.tapestry"
version = "1.0.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

gradlePlugin {
    plugins.create("me.chrr.tapestry.gradle") {
        id = "me.chrr.tapestry.gradle"
        displayName = "Tapestry Gradle"
        implementationClass = "me.chrr.tapestry.gradle.TapestryPlugin"
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
    implementation(plugin("net.fabricmc.fabric-loom", "1.14-SNAPSHOT"))
    implementation(plugin("net.neoforged.moddev", "2.0.134"))
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
        from(sourceSets.named("api").map { it.output })
    }

    getByName<Jar>("jar") {
        into("META-INF/jars") {
            from(apiJar)
            rename { "api.jar" }
        }
    }
}