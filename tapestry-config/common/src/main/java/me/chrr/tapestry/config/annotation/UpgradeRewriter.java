package me.chrr.tapestry.config.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Mark a function as an upgrade rewriter, which is called when the version stored within a config file is lower than
/// {@link UpgradeRewriter#currentVersion}. An upgrade rewriter manages changes to the schema of a configuration file,
/// allowing mods to e.g. rename options, change value types or remove options altogether.
///
/// The annotated function should have a signature matching something like
/// `void upgrade(int fromVersion, JsonObject config)`, taking in the current version of the config, and a raw JSON
/// object, which can be modified in-place.
///
/// @see me.chrr.tapestry.config.ConfigIo.UpgradeRewriter#upgrade
@NullMarked
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UpgradeRewriter {
    /// The current version of the config schema that is defined within the surrounding class.
    int currentVersion();
}
