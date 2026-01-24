package me.chrr.tapestry.base;

import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public interface PlatformMethods {
    Path getConfigDirectory();

    Path getGameDirectory();

    boolean isModLoaded(String id);
}