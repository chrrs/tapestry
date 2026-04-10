package me.chrr.tapestry.config;

import me.chrr.tapestry.config.value.Value;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// An option in a configuration file, which both describes the option's properties, and contains it's current value.
/// This is mostly an implementation detail constructed by {@link ReflectedConfig}.
@NullMarked
@ApiStatus.Internal
public class Option<T> {
    public @Nullable String serializedName;
    public Component displayName = Component.empty();
    public @Nullable Component header;
    public boolean hidden;

    public final Value<T> value;

    public Option(Value<T> value) {
        this.value = value;
    }
}
