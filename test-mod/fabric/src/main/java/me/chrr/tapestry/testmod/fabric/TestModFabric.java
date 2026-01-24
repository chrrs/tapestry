package me.chrr.tapestry.testmod.fabric;

import me.chrr.tapestry.base.Tapestry;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import me.chrr.tapestry.testmod.TestMod;
import net.fabricmc.api.ModInitializer;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FabricEntrypoint("main")
public class TestModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TestMod.LOGGER.info("Game dir: {}", Tapestry.PLATFORM_METHODS.getGameDirectory());
    }
}
