package me.chrr.tapestry.config.value;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/// A value for a config option that is both readable and writable. This also defines how the value should be read and
/// written, constraints for it to be valid, and formatters to make it readable in a GUI.
@NullMarked
public abstract class Value<T> implements Supplier<T> {
    /// The formatter that is used when displaying this value in the options GUI.
    @ApiStatus.Internal
    public Function<T, Component> formatter = (v) -> Component.literal(v.toString());

    /// The defined constraint for this value that has to hold when configuring in the GUI. Note that this constraint is
    /// only applied in the config screen. When deserializing, the constraint is not checked.
    @ApiStatus.Internal
    public @Nullable Constraint<T> constraint;

    /// This is set to true if the value has a custom formatter set to override the default. This is used by
    /// ReflectedConfig to override the default formatter for enums.
    @ApiStatus.Internal
    public boolean didSetFormatter = false;


    // FIXME: add a text input widget.

    /// Define the range of this value, without a step size. This is only valid for number types. If the option's value
    /// is outside of this range, the config will not be able to be saved. Out-of-range values can still be loaded from
    /// a file.
    public Value<T> range(T min, T max) {
        this.constraint = new Constraint.Range<>(min, max, Optional.empty());
        return this;
    }

    /// Define the range of this value, along with a step size. This is only valid for number types. By defining a step
    /// size, this will turn the option into a slider. Out-of-range values can still be loaded from a file.
    public Value<T> range(T min, T max, T step) {
        this.constraint = new Constraint.Range<>(min, max, Optional.of(step));
        return this;
    }

    /// Define a formatter for this value, which changes how the value is displayed in the config GUI.
    public Value<T> formatter(Function<T, Component> f) {
        this.didSetFormatter = true;
        this.formatter = f;
        return this;
    }


    /// Get the options value.
    public abstract T get();

    /// Set the options value to the given value.
    public abstract void set(T value);

    /// Get the options default value, which is used when it's not loaded from the config file.
    public abstract T getDefaultValue();

    /// Get the class type of the value (which should match `T`).
    public abstract Class<T> getValueType();
}
