package me.chrr.tapestry.config;

import com.google.gson.*;
import me.chrr.tapestry.config.value.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// Utilities for loading configuration options from JSON files. This is used internally by {@link ReflectedConfig}, and
/// shouldn't be used by consuming mods.
@NullMarked
@ApiStatus.Internal
public class ConfigIo {
    private static final Logger LOGGER = LogManager.getLogger("Tapestry/ConfigIo");

    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setFormattingStyle(FormattingStyle.PRETTY)
            .create();

    private ConfigIo() {
    }

    /// Load the given configuration options from the given file path. If that file path doesn't exist, try to migrate
    /// the configuration from one of the aliases instead. If the loaded configuration can be upgraded (according to
    /// {@link Config#getUpgradeRewriter()}), upgrade it before loading the values. If this fails, it will log to the
    /// console, and leave the config in an undefined state.
    public static void loadFromPathOrSaveDefault(Config config, Path file, List<Path> aliases) {
        if (Files.exists(file)) {
            loadFromPath(config, file);
            return;
        }

        for (Path alias : aliases) {
            if (!Files.isRegularFile(alias))
                continue;

            loadFromPath(config, alias);
            saveToPath(config, file);

            try {
                Files.delete(alias);
            } catch (IOException e) {
                LOGGER.error("Failed to delete old config file at '{}'", alias, e);
            }

            LOGGER.info("Migrated config from '{}' to '{}'", alias, file);
            return;
        }

        LOGGER.info("No config file found, saving default config to '{}'", file);
        saveToPath(config, file);
    }

    /// Load the given configuration options from the given file path, upgrading it if possible. If this fails, it will
    /// log to the console, and leave the config in an undefined state.
    private static void loadFromPath(Config config, Path path) {
        try {
            JsonObject object = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            UpgradeRewriter upgradeRewriter = config.getUpgradeRewriter();
            if (upgradeRewriter != null && object.has("version")) {
                int currentVersion = object.get("version").getAsInt();

                if (currentVersion < upgradeRewriter.getLatestVersion()) {
                    upgradeRewriter.upgrade(currentVersion, object);
                    object.addProperty("version", upgradeRewriter.getLatestVersion());

                    String json = GSON.toJson(object);
                    Files.writeString(path, json);

                    LOGGER.info("Upgraded config at '{}' to version {}", path, upgradeRewriter.getLatestVersion());
                }
            }

            for (Option<?> option : config.getOptions()) {
                if (option.serializedName != null && object.has(option.serializedName)) {
                    readIntoValue(option.value, object.get(option.serializedName));
                }
            }
        } catch (JsonSyntaxException | IOException e) {
            LOGGER.error("Failed to load config from '{}'", path, e);
        }
    }

    /// Try to read the given JSON value into the given config value, throwing an exception if this fails.
    private static <T> void readIntoValue(Value<T> value, JsonElement element) throws JsonSyntaxException {
        value.set(GSON.fromJson(element, value.getValueType()));
    }

    /// Try to save the given config to the given path. If this fails, it will log to the console and return.
    public static void saveToPath(Config config, Path path) {
        JsonObject object = new JsonObject();

        UpgradeRewriter upgradeRewriter = config.getUpgradeRewriter();
        if (upgradeRewriter != null) {
            object.addProperty("version", upgradeRewriter.getLatestVersion());
        }

        for (Option<?> option : config.getOptions()) {
            if (option.serializedName != null) {
                object.add(option.serializedName, GSON.toJsonTree(option.value.get(), option.value.getValueType()));
            }
        }

        try {
            String json = GSON.toJson(object);
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to '{}'", path, e);
        }
    }

    /// An upgrade rewriter manages changes to the schema of a configuration file, allowing mods to e.g. rename options,
    /// change value types or remove options altogether. This is usually implemented in {@link ReflectedConfig} using
    /// the {@link me.chrr.tapestry.config.annotation.UpgradeRewriter} annotation.
    @NullMarked
    @ApiStatus.Internal
    public interface UpgradeRewriter {
        /// An upgrade function that is run when the version property in a configuration file is lower than what
        /// {@link UpgradeRewriter#getLatestVersion()} returns. This is passed a raw JSON object, which can be edited
        /// in-place to perform modifications.
        void upgrade(int fromVersion, JsonObject config);

        /// Return the version number of the latest schema of the configuration file.
        int getLatestVersion();
    }
}
