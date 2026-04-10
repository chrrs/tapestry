package me.chrr.tapestry.config.value;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// A tracked value is a value that is serialized to JSON when saved.
@NullMarked
@ApiStatus.Internal
public class TrackedValue<T> extends Value<T> {
    private final Class<T> type;
    private final T defaultValue;
    public T value;

    public TrackedValue(Class<T> type, T defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }

    @Override
    public T getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Class<T> getValueType() {
        return this.type;
    }
}
