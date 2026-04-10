package me.chrr.tapestry.config.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Show a header in the generated config screen above the annotated option. The value passed to this annotation is part
/// of a translation key, see {@link TranslationPrefix}.
@NullMarked
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Category {
    String value();
}
