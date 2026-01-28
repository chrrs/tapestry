package me.chrr.tapestry.base.neoforge;

import me.chrr.tapestry.base.PlatformMethods;
import me.chrr.tapestry.gradle.annotation.Implementation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

@NullMarked
@Implementation("tapestry:platform_methods")
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
    public @Nullable String getModVersion(String modId) {
        IModFileInfo modFile = ModList.get().getModFileById(modId);

        if (modFile == null)
            return null;
        return modFile.versionString();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
