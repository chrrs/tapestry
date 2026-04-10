package me.chrr.tapestry.config;

import org.jspecify.annotations.NullMarked;

import java.util.Arrays;

/// A naming strategy that can be used to transform names from one strategy to another.
@NullMarked
public enum NamingStrategy {
    KEEP,
    SNAKE_CASE,
    CAMEL_CASE;

    /// Transform the given name into the name matching this naming strategy.
    public String transform(String name) {
        return switch (this) {
            case KEEP -> name;
            case SNAKE_CASE -> String.join("_", splitName(name)).toLowerCase();
            case CAMEL_CASE -> {
                StringBuilder builder = new StringBuilder();
                String[] words = splitName(name);
                builder.append(words[0]);
                Arrays.stream(words).skip(1).forEach((word) -> builder.append(capitalize(word)));
                yield builder.toString();
            }
        };
    }

    /// Split the given names roughly at their word boundaries, no matter its current naming strategy.
    private static String[] splitName(String name) {
        return name
                .replaceAll("[^\\p{Alnum}]+", " ")
                .replaceAll("([\\p{Lower}\\d])(\\p{Upper})", "$1 $2")
                .replaceAll("(\\p{Upper}+)(\\p{Upper}\\p{Lower})", "$1 $2")
                .replaceAll("(\\p{Alpha})(\\d)|(\\d)(\\p{Alpha})", "$1$3 $2$4")
                .trim()
                .split("\\s+");
    }

    /// Capitalise the first letter of the given string, and convert the rest to lowercase.
    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
