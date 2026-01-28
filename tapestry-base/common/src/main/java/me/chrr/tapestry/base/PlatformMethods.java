package me.chrr.tapestry.base;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

@NullMarked
public interface PlatformMethods {
    Path getConfigDirectory();

    Path getGameDirectory();

    @Nullable String getModVersion(String modId);

    boolean isModLoaded(String modId);
}