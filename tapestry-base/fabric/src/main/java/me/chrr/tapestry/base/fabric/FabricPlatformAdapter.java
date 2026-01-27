package me.chrr.tapestry.base.fabric;

import me.chrr.tapestry.base.PlatformAdapter;
import me.chrr.tapestry.base.Tapestry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
@SuppressWarnings("unused")
public class FabricPlatformAdapter implements PlatformAdapter {
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<Class<T>> findImplementations(Identifier identifier) {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            try {
                CustomValue tapestry = mod.getMetadata().getCustomValue("tapestry");
                if (tapestry == null)
                    continue;

                CustomValue implementations = tapestry.getAsObject().get("implementations");
                if (implementations == null)
                    continue;

                CustomValue array = implementations.getAsObject().get(identifier.toString());
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
