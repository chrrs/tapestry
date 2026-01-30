# Tapestry

> Useful utilities for multi-loader mod development.

![Version](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.chrr.me%2Freleases%2Fme%2Fchrr%2Ftapestry%2Ftapestry-gradle%2Fmaven-metadata.xml&style=flat-square&label=gradle)
![Version](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.chrr.me%2Freleases%2Fme%2Fchrr%2Ftapestry%2Ftapestry-base%2Fmaven-metadata.xml&style=flat-square&label=base)
![Version](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.chrr.me%2Freleases%2Fme%2Fchrr%2Ftapestry%2Ftapestry-config%2Fmaven-metadata.xml&style=flat-square&label=config)
![Build status](https://img.shields.io/github/actions/workflow/status/chrrs/tapestry/build.yml?style=flat-square)

For every mod developer, there comes a point in time where they get fed up with the status quo, and reinvent the world.
With that being said, here's some cool libraries to make building multi-loader mods a lot easier!

## Modules

- **`tapestry-gradle`:** A Gradle plugin that's capable of building universal JARs that work on both Fabric and
  Neoforge, thanks to Mojang deobfuscating the game! It uses Loom and ModDevGradle under the hood, and also does some
  more cool things, such as automatically generating mod manifests for you.
- **`tapestry-base`:** A small mod with some core utilities useful for any project and most of Tapestry's own modules.
- **`tapestry-config`:** A simple annotation-based config library, that can automatically generate both a config manager
  and a settings screen. It auto-detects translations, and is flexible enough to migrate legacy configs.
