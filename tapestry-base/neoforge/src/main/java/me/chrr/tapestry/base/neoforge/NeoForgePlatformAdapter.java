package me.chrr.tapestry.base.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import me.chrr.tapestry.base.PlatformAdapter;
import me.chrr.tapestry.base.Tapestry;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
@SuppressWarnings("unused")
public class NeoForgePlatformAdapter implements PlatformAdapter {
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<Class<T>> findImplementations(Identifier identifier) {
        for (IModInfo mod : ModList.get().getMods()) {
            try {
                Object tapestry = mod.getModProperties().get("tapestry");
                if (tapestry == null)
                    continue;

                Object implementations = ((CommentedConfig) tapestry).get("implementations");
                if (implementations == null)
                    continue;

                Object array = ((CommentedConfig) implementations).get(identifier.toString());
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
