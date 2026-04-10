package me.chrr.tapestry.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/// An in-memory configuration file with a list of options, its values and information on how to display them. The only
/// implementation of this interface at the moment is {@link ReflectedConfig}.
@NullMarked
public interface Config {
    /// Return the options defined in this configuration.
    @ApiStatus.Internal
    Collection<Option<?>> getOptions();

    /// Return the title that should be displayed in the GUI when editing this configuration.
    @ApiStatus.Internal
    Component getTitle();

    /// Save the configuration so it's persistent until the next time it's loaded again. If this fails, it should log to
    /// the console, and return gracefully.
    // FIXME: make this return a boolean or throw when it fails, so we can actually do something with it.
    void save();

    /// Return the upgrade rewriter that should be used when loading this configuration.
    @ApiStatus.Internal
    default ConfigIo.@Nullable UpgradeRewriter getUpgradeRewriter() {
        return null;
    }
}
