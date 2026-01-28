package me.chrr.tapestry.base.fabric;

import me.chrr.tapestry.base.PlatformMethods;
import me.chrr.tapestry.gradle.annotation.Implementation;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

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
    public @Nullable String getModVersion(String modId) {
        Optional<ModContainer> opt = FabricLoader.getInstance().getModContainer(modId);

        return opt.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
