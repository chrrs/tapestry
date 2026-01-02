package me.chrr.tapestry.base.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import me.chrr.tapestry.base.Tapestry;
import me.chrr.tapestry.base.platform.TapestryPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@NullMarked
@Mod("tapestry")
public class TapestryNeoForge {
    public TapestryNeoForge(IEventBus modBus) {
        modBus.register(this);
    }

    @SubscribeEvent
    public void onConstructMod(FMLConstructModEvent event) {
        for (IModInfo mod : ModList.get().getMods()) {
            try {
                Object properties = mod.getModProperties().get("tapestry");
                if (properties == null)
                    continue;

                CommentedConfig config = (CommentedConfig) properties;
                Object platformImplementations = config.get("platformImplementations");
                if (platformImplementations == null)
                    continue;

                //noinspection unchecked: we can't really check this.
                for (CommentedConfig object : (List<CommentedConfig>) platformImplementations) {
                    String class_ = object.get("class");
                    String implements_ = object.get("implements");

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
                Tapestry.LOGGER.error("Failed to parse tapestry block for '{}'", mod.getDisplayName());
            }
        }
    }
}
