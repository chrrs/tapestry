package me.chrr.tapestry.testmod;

import com.google.gson.JsonObject;
import me.chrr.tapestry.config.NamingStrategy;
import me.chrr.tapestry.config.ReflectedConfig;
import me.chrr.tapestry.config.annotation.Header;
import me.chrr.tapestry.config.annotation.Hidden;
import me.chrr.tapestry.config.annotation.SerializeName;
import me.chrr.tapestry.config.annotation.UpgradeRewriter;
import me.chrr.tapestry.config.value.Value;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SerializeName("client")
@SerializeName.Strategy(NamingStrategy.SNAKE_CASE)
@SuppressWarnings("unused")
public class TestModConfig extends ReflectedConfig {
    @Hidden
    public Value<Integer> pagesToShow = value(1);

    @Header("appearance")
    public Value<Boolean> doublePageViewing = map(pagesToShow, (n) -> n > 1, (b) -> b ? 2 : 1);
    public Value<Boolean> centerBookGui = value(true);
    public Value<Boolean> showFormattingButtons = value(true);
    public Value<ShowActionButtons> showActionButtons = value(ShowActionButtons.WHEN_EDITING);

    @Header("behaviour")
    public Value<Boolean> copyFormattingCodes = value(true);
    public Value<Integer> editHistorySize = value(32)
            .range(8, 128, 1);

    @Header("miscellaneous")
    public Value<Boolean> openVanillaBookScreenOnShift = value(false);

    @UpgradeRewriter(currentVersion = 3)
    public static void upgrade(int fromVersion, JsonObject config) {
        if (fromVersion < 3) {
            config.addProperty("show_action_buttons",
                    config.get("show_save_load_buttons").getAsBoolean() ? "WHEN_EDITING" : "NEVER");
        }
    }

    public enum ShowActionButtons {
        ALWAYS,
        WHEN_EDITING,
        NEVER,
    }
}
