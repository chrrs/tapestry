package me.chrr.tapestry.base;

import me.chrr.tapestry.gradle.annotation.Implementation;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/// The main class of the Tapestry Base module. This is mainly used for finding platform implementations and other
/// common platform methods.
@NullMarked
public class Tapestry {
    /// The platform adapter for the current loader. Note that this can't be loaded using annotated implementations,
    /// since they aren't initialized yet (that's the point of the adapters).
    private static final PlatformAdapter PLATFORM_ADAPTER;

    static {
        // We try to identify the current loader by looking at what classes are loaded. This is
        // not the most straight-forward option, but it's the best we have:
        // - I don't want to depend on a 'pre-launch' task, since that means there could be
        //   a point in time where platform implementations aren't loaded for other mods.
        // - Services don't work with multi-loader merged jars, as all implementations exist.
        try {
            if (isClassLoaded("net.fabricmc.loader.api.FabricLoader")) {
                Class<?> clazz = Class.forName("me.chrr.tapestry.base.fabric.FabricPlatformAdapter");
                PLATFORM_ADAPTER = (PlatformAdapter) clazz.getConstructor().newInstance();
            } else if (isClassLoaded("net.neoforged.neoforge.common.NeoForge")) {
                Class<?> clazz = Class.forName("me.chrr.tapestry.base.neoforge.NeoForgePlatformAdapter");
                PLATFORM_ADAPTER = (PlatformAdapter) clazz.getConstructor().newInstance();
            } else {
                throw new IllegalStateException("Tapestry isn't loaded on a supported platform");
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load platform adapter", exception);
        }
    }

    // -- Platform adapter needs to be initialised after this, so the slightly awkward formatting here is needed.

    /// The logger for all Tapestry-related modules. This shouldn't be used by consuming mods.
    @ApiStatus.Internal
    public static final Logger LOGGER = LogManager.getLogger("Tapestry");

    /// Common platform method implementations that are often used in mods, see {@link PlatformMethods}.
    public static final PlatformMethods PLATFORM_METHODS = implementation(Identifier.fromNamespaceAndPath("tapestry", "platform_methods"));

    private Tapestry() {
    }

    /// Find a platform implementation for the given interface identifier. When building using Tapestry Gradle,
    /// implementations can be marked with the {@link Implementation} annotation, which will make them discoverable
    /// using this method automatically.
    public static <T> T implementation(Identifier iface) {
        List<Class<T>> classes = PLATFORM_ADAPTER.findImplementations(iface);
        Validate.isTrue(!classes.isEmpty(), "No entrypoints found for " + iface);
        Validate.isTrue(classes.size() == 1, "More than one entrypoint found for " + iface);

        try {
            return classes.getFirst().getConstructor().newInstance();
        } catch (Exception exception) {
            throw new RuntimeException("Couldn't instantiate implementation for " + iface, exception);
        }
    }

    /// Figure out if a JVM class with the given name is loaded in the current thread.
    private static boolean isClassLoaded(String name) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null)
                loader = ClassLoader.getSystemClassLoader();
            Class.forName(name, false, loader);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
