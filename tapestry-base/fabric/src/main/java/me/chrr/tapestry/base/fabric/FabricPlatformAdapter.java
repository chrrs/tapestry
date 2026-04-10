package me.chrr.tapestry.base.fabric;

import me.chrr.tapestry.base.PlatformAdapter;
import me.chrr.tapestry.base.Tapestry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/// The platform adapter for Fabric. This will find implementations of interfaces using the "tapestry" object in a mod's
/// custom properties in fabric.mod.json, which is under `custom.tapestry`.
///
/// @see <a href="https://wiki.fabricmc.net/documentation:fabric_mod_json">fabric.mod.json documentation</a>
@NullMarked
@ApiStatus.Internal
public class FabricPlatformAdapter implements PlatformAdapter {
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<Class<T>> findImplementations(Identifier iface) {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            try {
                CustomValue tapestry = mod.getMetadata().getCustomValue("tapestry");
                if (tapestry == null)
                    continue;

                CustomValue implementations = tapestry.getAsObject().get("implementations");
                if (implementations == null)
                    continue;

                CustomValue array = implementations.getAsObject().get(iface.toString());
                if (array == null)
                    continue;

                CustomValue.CvArray list = array.getAsArray();
                List<Class<T>> out = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    out.add((Class<T>) Class.forName(list.get(i).getAsString()));
                }

                return out;
            } catch (Exception exception) {
                Tapestry.LOGGER.error("Failed to parse tapestry block for '{}'", mod.getMetadata().getName());
            }
        }

        return List.of();
    }
}
