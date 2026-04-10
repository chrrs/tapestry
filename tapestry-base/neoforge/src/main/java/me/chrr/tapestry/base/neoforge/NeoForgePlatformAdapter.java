package me.chrr.tapestry.base.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import me.chrr.tapestry.base.PlatformAdapter;
import me.chrr.tapestry.base.Tapestry;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/// The platform adapter for NeoForge. This will find implementations of interfaces using the "tapestry" object in a
/// mod's custom properties in neoforge.mods.toml. It's not documented very clearly how custom mod properties work in
/// NeoForge, but they are found under `modproperties.[mod-id].tapestry`.
@NullMarked
@ApiStatus.Internal
public class NeoForgePlatformAdapter implements PlatformAdapter {
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<Class<T>> findImplementations(Identifier iface) {
        for (IModInfo mod : ModList.get().getMods()) {
            try {
                Object tapestry = mod.getModProperties().get("tapestry");
                if (tapestry == null)
                    continue;

                Object implementations = ((CommentedConfig) tapestry).get("implementations");
                if (implementations == null)
                    continue;

                Object array = ((CommentedConfig) implementations).get(iface.toString());
                if (array == null)
                    continue;

                List<String> list = (List<String>) array;
                List<Class<T>> out = new ArrayList<>(list.size());
                for (String name : list) {
                    out.add((Class<T>) Class.forName(name));
                }

                return out;
            } catch (Exception exception) {
                Tapestry.LOGGER.error("Failed to parse tapestry block for '{}'", mod.getDisplayName());
            }
        }

        return List.of();
    }
}
