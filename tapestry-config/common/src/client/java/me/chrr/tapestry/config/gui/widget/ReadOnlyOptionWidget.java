package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// The fallback option widget, which is not editable and only shows the current value of the option.
@NullMarked
@ApiStatus.Internal
public class ReadOnlyOptionWidget<T> extends OptionWidget<T> {
    public ReadOnlyOptionWidget(OptionProxy<T> optionProxy) {
        super(optionProxy);
        this.active = false;
    }

    @Override
    protected void extractOptionWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.extractOptionLabel(graphics, this.getWidth());
        this.extractValueLabel(graphics, 0, this.getWidth());
        this.handleCursor(graphics);
    }
}
