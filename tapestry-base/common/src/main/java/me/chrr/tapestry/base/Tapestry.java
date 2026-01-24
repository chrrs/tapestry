package me.chrr.tapestry.base;

import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class Tapestry {
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

    // -- Platform adapter needs to be initialised here, so the slightly awkward formatting is needed.
    public static final Logger LOGGER = LogManager.getLogger("Tapestry");
    public static final PlatformMethods PLATFORM_METHODS = implementation(PlatformMethods.class, id("platform_methods"));

    public static <T> T implementation(Class<T> clazz, Identifier identifier) {
        List<Class<T>> classes = PLATFORM_ADAPTER.findImplementations(clazz, identifier);
        Validate.isTrue(!classes.isEmpty(), "No entrypoints found for " + identifier);
        Validate.isTrue(classes.size() == 1, "More than one entrypoint found for " + identifier);

        try {
            return classes.getFirst().getConstructor().newInstance();
        } catch (Exception exception) {
            throw new RuntimeException("Couldn't instantiate implementation for " + identifier, exception);
        }
    }

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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("tapestry", path);
    }
}
