package me.chrr.tapestry.base.platform;

import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public interface PlatformMethods {
    Path getConfigDirectory();

    Path getGameDirectory();

    boolean isModLoaded(String id);


    static PlatformMethods get() {
        return TapestryPlatform.getImplementation(PlatformMethods.class);
    }
}
