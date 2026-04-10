package me.chrr.tapestry.config.annotation;

import me.chrr.tapestry.config.NamingStrategy;
import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Change the serialized name of the annotated option. Changing this will also affect the translation key. If this
/// isn't set, the serialized name will be determined from the field name using {@link SerializeName.Strategy}. See
/// {@link TranslationPrefix} for more specific details on how the translation key is constructed.
@NullMarked
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SerializeName {
    String value();

    /// Set the naming strategy that should be used when transforming the field name into the property name. This also
    /// affects the translation key. If this is used on the config class, it will set the default naming strategy for
    /// all the configuration options.
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    @interface Strategy {
        NamingStrategy value();
    }
}
