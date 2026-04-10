package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Option;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// An option proxy holds a new value for an option without applying it. This is used in the config screen, so that
/// changed options aren't applied immediately. It also manages if these values are changed, and can reset the values
/// to their default values.
@NullMarked
@ApiStatus.Internal
public class OptionProxy<T> {
    public final Option<T> option;
    public T value;

    public OptionProxy(Option<T> option) {
        this.option = option;
        this.value = option.value.get();
    }

    /// Apply the change to the config option.
    public void apply() {
        this.option.value.set(this.value);
    }

    /// Reset the option proxy to the options default value.
    public void resetToDefault() {
        this.value = this.option.value.getDefaultValue();
    }

    /// Returns true if the value stored in this proxy is different from the actual option value.
    public boolean isDirty() {
        return this.value != this.option.value.get();
    }

    /// Returns true if the value stored in this proxy is different from the default option value.
    public boolean isChanged() {
        return this.value != this.option.value.getDefaultValue();
    }
}
