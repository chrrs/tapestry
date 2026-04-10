package me.chrr.tapestry.base;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/// Platform adapters are implemented by loaders, which can load mods, whose mods can specify implementations for
/// interfaces. This is used to provide different implementations per loader for multi-platform mods. Platform adapters
/// are an alternative to JVM services, since those wouldn't work when the resource directory is shared.
@NullMarked
@ApiStatus.Internal
public interface PlatformAdapter {
    /// Return a list of all classes marked as implementations for the given interface identifier.
    <T> List<Class<T>> findImplementations(Identifier iface);
}
