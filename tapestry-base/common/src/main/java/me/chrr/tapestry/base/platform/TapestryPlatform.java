package me.chrr.tapestry.base.platform;

import me.chrr.tapestry.base.Tapestry;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
public class TapestryPlatform {
    private static final Map<Class<?>, Object> platformedImplementations = new HashMap<>();

    public static void registerImplementation(Class<?> clazz, Object implementation) {
        if (!clazz.isAssignableFrom(implementation.getClass()))
            throw new IllegalArgumentException("Platform implementation '" + implementation.getClass().getName() + "' is not assignable to '" + clazz.getName() + "'");
        if (platformedImplementations.containsKey(clazz))
            throw new IllegalStateException("Platformed class '" + clazz.getName() + "' has more than one implementation.");

        platformedImplementations.put(clazz, implementation);
        Tapestry.LOGGER.info("Registered platform implementation for '{}'", clazz.getName());
    }

    public static <T> T getImplementation(Class<T> clazz) {
        Object instance = platformedImplementations.get(clazz);
        if (instance == null)
            throw new RuntimeException("Platformed class '" + clazz.getName() + "' has no registered implementation.");

        //noinspection unchecked: this is checked when registering.
        return (T) instance;
    }
}
