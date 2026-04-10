package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Option;
import me.chrr.tapestry.config.gui.widget.*;
import me.chrr.tapestry.config.value.Constraint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/// A widget that shows a list of configuration options, their headers and option widgets.
@NullMarked
@ApiStatus.Internal
public class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {
    private final boolean showHeaderSeparator;

    public OptionList(Minecraft minecraft, boolean showHeaderSeparator, int width, int height, int y) {
        super(minecraft, width, height, y, 25);

        this.showHeaderSeparator = showHeaderSeparator;
        this.centerListVertically = false;
    }

    /// Add a header to the end of the option list.
    public void addHeader(Component text) {
        int padding = this.children().isEmpty() ? 8 : 16;
        int height = padding + OptionList.this.minecraft.font.lineHeight;
        this.addEntry(new HeaderEntry(text), height);
    }

    /// Add an option widget to the end of the option list, returning its proxy.
    public <T> OptionProxy<T> addOption(Option<T> option) {
        OptionProxy<T> proxy = new OptionProxy<>(option);
        this.addEntry(new OptionEntry<>(proxy));
        return proxy;
    }

    @Override
    public int getRowWidth() {
        return 310;
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor GuiGraphicsExtractor) {
        if (this.showHeaderSeparator) {
            Identifier headerSeparator = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
            GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, headerSeparator, this.getX(), this.getY() - 2, 0f, 0f, this.getWidth(), 2, 32, 2);
        }

        Identifier footerSeparator = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        GuiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, footerSeparator, this.getX(), this.getBottom(), 0f, 0f, this.getWidth(), 2, 32, 2);
    }

    /// Construct a suitable option widget for the given option, according to its type and constraint.
    private static <T> OptionWidget<T> getWidgetForProxy(OptionProxy<T> optionProxy) {
        Class<T> valueClass = optionProxy.option.value.getValueType();

        if (valueClass == boolean.class || valueClass == Boolean.class) {
            return unsafeCast(new BooleanOptionWidget(unsafeCast(optionProxy)));
        } else if (optionProxy.option.value.constraint instanceof Constraint.Range<T> range && range.step().isPresent()) {
            if ((valueClass == int.class || valueClass == Integer.class)) {
                return unsafeCast(new SliderOptionWidget.Int(unsafeCast(optionProxy), unsafeCast(range)));
            } else if ((valueClass == float.class || valueClass == Float.class)) {
                return unsafeCast(new SliderOptionWidget.Float(unsafeCast(optionProxy), unsafeCast(range)));
            } else {
                throw new IllegalArgumentException("Range constraint can't be applied to a value of type " + valueClass);
            }
        } else if (optionProxy.option.value.constraint instanceof Constraint.Values<T>(List<T> values)) {
            return new EnumOptionWidget<>(optionProxy, values);
        } else {
            return new ReadOnlyOptionWidget<>(optionProxy);
        }
    }

    /// Cast the given value from V to T without compile-time checks. Make sure this is right before using it!
    @SuppressWarnings("unchecked")
    private static <T, V> T unsafeCast(V value) {
        return (T) value;
    }

    /// A convenience "type alias" for a single entry within the option list.
    @NullMarked
    @ApiStatus.Internal
    protected abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
    }

    /// An entry in the option list that displays a simple header.
    @NullMarked
    @ApiStatus.Internal
    protected class HeaderEntry extends Entry {
        private final StringWidget widget;

        public HeaderEntry(Component text) {
            this.widget = new StringWidget(text, minecraft.font);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int x, int y, boolean bl, float delta) {
            this.widget.setPosition(this.getContentX() + 2, this.getContentBottom() - minecraft.font.lineHeight);
            this.widget.extractRenderState(graphics, x, y, delta);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }
    }

    /// An entry in the option list that displays the option widget for a single option, along with a reset button.
    @NullMarked
    @ApiStatus.Internal
    protected static class OptionEntry<T> extends Entry {
        private final OptionWidget<T> widget;
        private final OptionProxy<T> optionProxy;
        private final IconButton reset;

        public OptionEntry(OptionProxy<T> optionProxy) {
            this.widget = getWidgetForProxy(optionProxy);
            this.optionProxy = optionProxy;

            this.reset = new IconButton(0, 0, 0, 0,
                    Component.translatable("text.tapestry.config.reset_option"),
                    Identifier.fromNamespaceAndPath("tapestry_config", "textures/gui/reset.png"),
                    (_) -> optionProxy.resetToDefault());
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget, this.reset);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int x, int y, boolean isHovering, float delta) {
            this.widget.setPosition(this.getContentX(), this.getContentY());
            this.widget.setSize(this.getContentWidth() - this.getContentHeight() - 4, this.getContentHeight());
            this.widget.extractRenderState(graphics, x, y, delta);

            this.reset.active = this.optionProxy.isChanged();
            this.reset.setPosition(this.getContentRight() - this.getContentHeight(), this.getContentY());
            this.reset.setSize(this.getContentHeight(), this.getContentHeight());
            this.reset.extractRenderState(graphics, x, y, delta);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget, this.reset);
        }
    }
}
