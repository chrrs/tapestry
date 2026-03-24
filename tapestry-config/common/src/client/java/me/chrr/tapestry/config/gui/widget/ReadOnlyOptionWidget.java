package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.NullMarked;

@NullMarked
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
