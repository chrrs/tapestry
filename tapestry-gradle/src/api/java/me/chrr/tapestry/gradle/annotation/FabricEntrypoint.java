package me.chrr.tapestry.gradle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Specifies that the annotated class, field or method is a Fabric entrypoint. Tapestry Gradle will search for all
/// items annotated with this, and put according definitions in fabric.mod.json.
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface FabricEntrypoint {
    String[] value();
}
