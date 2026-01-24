package me.chrr.tapestry.testmod;

import me.chrr.tapestry.config.ReflectedConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestMod {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final TestModConfig CONFIG = ReflectedConfig.load(TestModConfig.class, "tapestry_test_mod.json");
}
