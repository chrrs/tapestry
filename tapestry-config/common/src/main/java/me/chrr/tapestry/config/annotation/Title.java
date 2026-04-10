package me.chrr.tapestry.config.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Set the title translation key that is displayed in the generated config screen, see {@link TranslationPrefix}.
@NullMarked
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Title {
    String value();
}
