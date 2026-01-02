package me.chrr.tapestry.base.fabric;

import me.chrr.tapestry.base.Tapestry;
import me.chrr.tapestry.base.platform.TapestryPlatform;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.InvocationTargetException;

@NullMarked
@FabricEntrypoint("preLaunch")
public class TapestryFabric implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            try {
                CustomValue tapestry = mod.getMetadata().getCustomValue("tapestry");
                if (tapestry == null)
                    continue;

                CustomValue implementations = tapestry.getAsObject().get("platformImplementations");
                if (implementations == null)
                    continue;

                for (CustomValue obj : implementations.getAsArray()) {
                    CustomValue.CvObject object = obj.getAsObject();

                    String class_ = object.get("class").getAsString();
                    String implements_ = object.get("implements").getAsString();

                    try {
                        Class<?> clazz = Class.forName(implements_);
                        Object instance = Class.forName(class_).getConstructor().newInstance();
                        TapestryPlatform.registerImplementation(clazz, instance);
                    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                             IllegalAccessException | InvocationTargetException exception) {
                        Tapestry.LOGGER.error("Unable to register '{}' as platform implementation for '{}'", class_, implements_, exception);
                    }
                }
            } catch (Exception exception) {
                Tapestry.LOGGER.error("Failed to parse tapestry block for '{}'", mod.getMetadata().getName());
            }
        }
    }
}
