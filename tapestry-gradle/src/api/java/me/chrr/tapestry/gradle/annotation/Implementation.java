package me.chrr.tapestry.gradle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Specifies that the given class is an implementation of a named interface. Tapestry Gradle will search for all types
/// annotated with this, and add them to the right location within the mod manifest. Implementations can be queried
/// using {@link me.chrr.tapestry.base.Tapestry#implementation}, which is included in `tapestry-base`.
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Implementation {
    String value();
}
