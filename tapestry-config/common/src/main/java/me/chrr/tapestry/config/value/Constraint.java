package me.chrr.tapestry.config.value;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

@NullMarked
public sealed interface Constraint<T> {
    record Range<T>(T min, T max, Optional<T> step) implements Constraint<T> {
    }

    record Values<T>(List<T> values) implements Constraint<T> {
    }
}
