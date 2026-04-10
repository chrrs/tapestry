package me.chrr.tapestry.base;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/// Common platform-specific methods that can be used by both Tapestry and consuming mods, accessible through
/// {@link me.chrr.tapestry.base.Tapestry#PLATFORM_METHODS}.
@NullMarked
public interface PlatformMethods {
    /// Return the directory in which mod configs should be stored.
    Path getConfigDirectory();

    /// Return the game directory.
    Path getGameDirectory();

    /// If the mod is present and loaded, return the loaded version of the given mod ID.
    @Nullable String getModVersion(String modId);

    /// Check if a mod with the given mod ID is present and loaded.
    boolean isModLoaded(String modId);
}