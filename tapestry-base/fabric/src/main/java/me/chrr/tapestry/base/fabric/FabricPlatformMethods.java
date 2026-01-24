package me.chrr.tapestry.base.fabric;

import me.chrr.tapestry.base.PlatformMethods;
import me.chrr.tapestry.gradle.annotation.Implementation;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
@Implementation("tapestry:platform_methods")
public class FabricPlatformMethods implements PlatformMethods {
    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getGameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
