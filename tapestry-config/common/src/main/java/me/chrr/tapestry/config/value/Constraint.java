package me.chrr.tapestry.config.value;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/// Defines a constraint an option value should hold. The constraint is used to determine which
/// {@link me.chrr.tapestry.config.gui.widget.OptionWidget} should be shown for an option in the generated config
/// screen, and is not actually validated at any point.
@NullMarked
@ApiStatus.Internal
public sealed interface Constraint<T> {
    /// Defines that a value should exist within the given range. If the step size is defined, this will turn the option
    /// into a slider.
    @NullMarked
    @ApiStatus.Internal
    record Range<T>(T min, T max, Optional<T> step) implements Constraint<T> {
    }

    /// Defines that a value has a list of set values it can be. This is mainly used for enums.
    @NullMarked
    @ApiStatus.Internal
    record Values<T>(List<T> values) implements Constraint<T> {
    }
}
