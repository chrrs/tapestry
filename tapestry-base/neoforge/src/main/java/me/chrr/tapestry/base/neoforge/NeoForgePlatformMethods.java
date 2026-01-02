package me.chrr.tapestry.base.neoforge;

import me.chrr.tapestry.base.platform.PlatformMethods;
import me.chrr.tapestry.gradle.annotation.PlatformImplementation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
@PlatformImplementation(PlatformMethods.class)
public class NeoForgePlatformMethods implements PlatformMethods {
    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getGameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}
