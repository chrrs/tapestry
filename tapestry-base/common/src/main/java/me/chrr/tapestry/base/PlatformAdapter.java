package me.chrr.tapestry.base;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public interface PlatformAdapter {
    <T> List<Class<T>> findImplementations(Identifier identifier);
}
