package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A simple boolean option widget, which cycles through the available options.
@NullMarked
@ApiStatus.Internal
public class EnumOptionWidget<T> extends OptionWidget.Clickable<T> {
    private final List<T> values;
    private final Map<T, Component> nameByValue = new HashMap<>();

    public EnumOptionWidget(OptionProxy<T> optionProxy, List<T> values) {
        super(optionProxy);

        this.values = values;
        for (T value : values)
            this.nameByValue.put(value, optionProxy.option.value.formatter.apply(value));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        optionProxy.value = values.get((values.indexOf(optionProxy.value) + 1) % values.size());
    }

    @Override
    protected void extractOptionWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.extractOptionLabel(graphics, this.getWidth());
        this.extractValueLabel(graphics, 0, this.getWidth(), nameByValue.get(optionProxy.value));
    }
}
