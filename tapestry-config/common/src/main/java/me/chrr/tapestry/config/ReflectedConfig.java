package me.chrr.tapestry.config;

import com.google.gson.JsonObject;
import me.chrr.tapestry.base.Tapestry;
import me.chrr.tapestry.config.annotation.*;
import me.chrr.tapestry.config.value.Constraint;
import me.chrr.tapestry.config.value.TrackedValue;
import me.chrr.tapestry.config.value.Value;
import me.chrr.tapestry.config.value.VirtualValue;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/// A configuration file with options derived from fields defined in the implementing class. The class itself can be
/// annotated with a few options, changing its behaviour:
///
/// - {@link TranslationPrefix}: The prefix to prepend to all option names, header names and the config title, which
///   will then be passed into {@link Component#translatable}.
/// - {@link Title}: The title of the config class, which is used to determine the translation key of the GUI.
/// - {@link SerializeName.Strategy}: The default name transformation strategy used for the configuration options.
///
/// ---
///
/// Any public, non-static, non-transient field with type {@link Value} will be reflected. These fields can be created
/// using the helper methods {@link ReflectedConfig#value} and {@link ReflectedConfig#map}. Fields can also be annotated
/// with various annotations to modify their behaviour:
///
/// - {@link Category}: Show a header above the annotated option, useful for separating categories.
/// - {@link Hidden}: Hide the annotated option from the config GUI, while still serializing it.
/// - {@link SerializeName} and {@link SerializeName.Strategy}: Change the serialized property name of the option. This
///   is also used to determine the options translation key.
///
/// ---
///
/// A single method can be defined as the "upgrade rewriter" using the {@link UpgradeRewriter} annotation. This method
/// will be called when the version property within the JSON config is lower than the defined latest version. In that
/// case, the upgrade method will be called, at which point it can apply manual upgrade rules to make config files
/// compatible with the current format defined by the fields.
///
/// A visual configuration GUI can be created through {@link me.chrr.tapestry.config.gui.TapestryConfigScreen}. For this
/// to look right, you'll probably want to set up proper translations, which should be done using the
/// {@link TranslationPrefix} annotation on the config class. See the annotation Javadoc for more information on that.
///
/// ---
///
/// To load a reflected config, the {@link ReflectedConfig#load} function should be used. The variable returned can ex.
/// be stored in a public static variable in the mods main class.
///
/// @see <a href="https://github.com/chrrs/tapestry/blob/main/test-mod/common/src/main/java/me/chrr/tapestry/testmod/TestModConfig.java">Example config</a>
@NullMarked
public abstract class ReflectedConfig implements Config {
    private final List<Option<?>> options = new ArrayList<>();

    private Component title = Component.empty();
    private ConfigIo.@Nullable UpgradeRewriter upgradeRewriter = null;

    private @Nullable Path currentConfigPath = null;
    private @Nullable TranslationPrefix translationPrefix = null;

    //region Value constructors

    /// Define a new simple, tracked value, which will store its own value and is serialized to the config file. If it's
    /// not loaded yet (or is initialised to be default), it will have the passed value.
    @SuppressWarnings("unchecked")
    protected <T> Value<T> value(T defaultValue) {
        return new TrackedValue<>((Class<T>) defaultValue.getClass(), defaultValue);
    }

    /// Define a new mapped value, which is derived from another value and transformed using the given functions.
    @SuppressWarnings("unchecked")
    protected <U, V> Value<V> map(Value<U> value, Function<U, V> aToB, Function<V, U> bToA) {
        V defaultValue = aToB.apply(value.getDefaultValue());

        return new VirtualValue<>((Class<V>) defaultValue.getClass(), defaultValue,
                () -> aToB.apply(value.get()), (v) -> value.set(bToA.apply(v)));
    }
    //endregion

    //region Initialization & reflection

    /// Reflect all the config options and attributes from the subclass.
    private void reflectOptions() {
        this.upgradeRewriter = reflectUpgradeRewriter();
        this.translationPrefix = getClass().getAnnotation(TranslationPrefix.class);

        // Get the default naming strategy.
        NamingStrategy namingStrategy = NamingStrategy.SNAKE_CASE;
        SerializeName.Strategy serializeNameStrategyAnnotation = getClass().getAnnotation(SerializeName.Strategy.class);
        if (serializeNameStrategyAnnotation != null)
            namingStrategy = serializeNameStrategyAnnotation.value();

        // Get the config screen title.
        Title titleAnnotation = getClass().getAnnotation(Title.class);
        this.title = titleAnnotation != null
                ? getTranslatedName(titleAnnotation.value() + ".title")
                : getTranslatedName("title");

        // Construct options for all public, non-static, non-transient fields.
        for (Field field : getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))
                continue;
            this.options.add(reflectOptionFromField(field, namingStrategy));
        }
    }

    /// If it's present, try to reflect the upgrade rewriter from the subclass.
    private ConfigIo.@Nullable UpgradeRewriter reflectUpgradeRewriter() {
        ConfigIo.UpgradeRewriter upgradeRewriter = null;

        for (Method method : getClass().getDeclaredMethods()) {
            UpgradeRewriter annotation = method.getAnnotation(UpgradeRewriter.class);
            if (annotation == null)
                continue;

            if (!Modifier.isStatic(method.getModifiers()))
                throw new IllegalArgumentException("Upgrade rewriter '" + method.getName() + "' is not static");

            if (upgradeRewriter != null)
                throw new IllegalArgumentException("Config class '" + getClass().getName() + "' defines more than one upgrade rewriter");

            method.setAccessible(true);
            upgradeRewriter = new ConfigIo.UpgradeRewriter() {
                @Override
                public void upgrade(int fromVersion, JsonObject config) {
                    try {
                        method.invoke(null, fromVersion, config);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke upgrade rewriter", e);
                    }
                }

                @Override
                public int getLatestVersion() {
                    return annotation.currentVersion();
                }
            };
        }

        return upgradeRewriter;
    }

    /// Reflect a single config option from the given field.
    private Option<?> reflectOptionFromField(Field field, NamingStrategy defaultNamingStrategy) {
        try {
            Class<?> type = field.getType();
            if (!Value.class.isAssignableFrom(type))
                throw new IllegalArgumentException("All (non-transient) public fields in a config class should be Value<?>, which " + type + " isn't");

            Value<?> value = (Value<?>) field.get(this);
            return reflectOptionFromValue(value, field, defaultNamingStrategy);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't initialize config field " + field);
        }
    }

    /// Reflect a single config option from the given field, with the given resolved value instance.
    private <T> Option<T> reflectOptionFromValue(Value<T> value, Field field, NamingStrategy defaultNamingStrategy) {
        Option<T> option = new Option<>(value);

        SerializeName serializeName = field.getAnnotation(SerializeName.class);
        SerializeName.Strategy strategy = getClass().getAnnotation(SerializeName.Strategy.class);
        NamingStrategy namingStrategy = strategy != null ? strategy.value() : defaultNamingStrategy;

        // Serialization options.
        if (value instanceof TrackedValue<T> trackedValue) {
            option.serializedName = serializeName != null ? serializeName.value() : namingStrategy.transform(field.getName());

            // Fill in some value properties if it's an enum.
            Class<T> valueType = trackedValue.getValueType();
            if (valueType.isEnum()) {
                if (value.constraint == null)
                    value.constraint = new Constraint.Values<>(Arrays.asList(valueType.getEnumConstants()));

                if (!value.didSetFormatter)
                    value.formatter = (v) -> getTranslatedName(
                            "value." + namingStrategy.transform(valueType.getSimpleName())
                                    + "." + namingStrategy.transform(v.toString()));
            }
        }

        // GUI-specific options.
        if (field.isAnnotationPresent(Hidden.class)) {
            option.hidden = true;
        } else {
            option.displayName = getTranslatedName("option." + namingStrategy.transform(field.getName()));
        }

        Category category = field.getAnnotation(Category.class);
        if (category != null)
            option.header = getTranslatedName("category." + category.value());

        return option;
    }

    /// Get the (possibly) translated display name for the given key, which is prefixed with the translation prefix if
    /// it exists, and otherwise is returned as a literal text component.
    private Component getTranslatedName(String key) {
        if (this.translationPrefix == null) {
            return Component.literal(key);
        } else {
            return Component.translatable(this.translationPrefix.value() + "." + key);
        }
    }
    //endregion

    //region Loading

    /// Load a configuration file of the given class from the file in the game config dir with the given file name. If
    /// it doesn't exist, this will resolve to the default configuration, and save that to the disk.
    public static <T extends ReflectedConfig> T load(Class<T> configClass, String file) {
        return load(configClass, file, List.of());
    }

    /// Load a configuration file of the given class from the file in the game config dir with the given file name. If
    /// this file isn't found, it will try to migrate the config from one of the given alias files. If none of those
    /// exist either, this will resolve to the default configuration, and save that to the disk.
    public static <T extends ReflectedConfig> T load(Class<T> configClass, String file, List<String> aliases) {
        try {
            Constructor<T> constructor = configClass.getConstructor();
            constructor.setAccessible(true);

            T config = constructor.newInstance();
            ((ReflectedConfig) config).reflectOptions();

            Path confDir = Tapestry.PLATFORM_METHODS.getConfigDirectory();
            Path configFile = confDir.resolve(file);
            ((ReflectedConfig) config).currentConfigPath = configFile;

            ConfigIo.loadFromPathOrSaveDefault(config, configFile,
                    aliases.stream().map(confDir::resolve).toList());

            return config;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Config class '" + configClass.getName() + "' does not have a default constructor", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Config class '" + configClass.getName() + "' could not be instantiated", e);
        }
    }
    //endregion

    //region Interface implementations
    @Override
    public Collection<Option<?>> getOptions() {
        return this.options;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public void save() {
        ConfigIo.saveToPath(this, Objects.requireNonNull(this.currentConfigPath));
    }

    @Override
    public ConfigIo.@Nullable UpgradeRewriter getUpgradeRewriter() {
        return this.upgradeRewriter;
    }
    //endregion
}
