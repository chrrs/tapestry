package me.chrr.tapestry.testmod.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.chrr.tapestry.config.gui.TapestryConfigScreen;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import me.chrr.tapestry.testmod.TestMod;

@FabricEntrypoint("modmenu")
public class TestModModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (parent) -> new TapestryConfigScreen(parent, TestMod.CONFIG);
    }
}
