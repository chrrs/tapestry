package me.chrr.tapestry.testmod.neoforge;

import me.chrr.tapestry.base.Tapestry;
import me.chrr.tapestry.config.gui.TapestryConfigScreen;
import me.chrr.tapestry.testmod.TestMod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Mod("tapestry_test_mod")
public class TestModNeoForge {
    public TestModNeoForge(ModContainer mod) {
        TestMod.LOGGER.info("Game dir: {}", Tapestry.PLATFORM_METHODS.getGameDirectory());

        mod.registerExtensionPoint(IConfigScreenFactory.class,
                (_, parent) -> new TapestryConfigScreen(parent, TestMod.CONFIG));
    }
}
