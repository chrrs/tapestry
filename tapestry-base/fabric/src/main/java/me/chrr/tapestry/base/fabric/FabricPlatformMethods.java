package me.chrr.tapestry.base.fabric;

import me.chrr.tapestry.base.platform.PlatformMethods;
import me.chrr.tapestry.gradle.annotation.PlatformImplementation;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
@PlatformImplementation(PlatformMethods.class)
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
