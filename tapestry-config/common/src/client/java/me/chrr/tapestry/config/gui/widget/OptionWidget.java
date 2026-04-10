package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/// An option widget allows the user to change a config option, according to its type and constraints. It renders as a
/// normal Minecraft button, but with the option name at the left, and a widget at the right.
@NullMarked
@ApiStatus.Internal
public abstract class OptionWidget<T> extends AbstractWidget {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("widget/button"),
            Identifier.withDefaultNamespace("widget/button_disabled"),
            Identifier.withDefaultNamespace("widget/button_highlighted")
    );

    public final OptionProxy<T> optionProxy;
    public final WidgetTooltipHolder customTooltip;

    public OptionWidget(OptionProxy<T> optionProxy) {
        super(0, 0, 0, 0, optionProxy.option.displayName);

        this.optionProxy = optionProxy;
        this.customTooltip = new WidgetTooltipHolder();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    /// Render the option label at the left of the widget, with the given maximum width. If the option is dirty, this
    /// will render the label in italic, and append a star after it.
    protected void extractOptionLabel(GuiGraphicsExtractor graphics, int availableWidth) {
        ActiveTextCollector textCollector = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE);

        Component message = this.getMessage();
        if (this.optionProxy.isDirty())
            message = Component.empty().append(message).append(" *").withStyle(ChatFormatting.ITALIC);

        textCollector.acceptScrolling(message,
                this.getX() + 2 + 4, this.getX() + 2 + 4, this.getX() + availableWidth - 2 - 4,
                this.getY() + 2, this.getBottom() - 2);
    }

    /// Render a simple value label at the given offset from the right, with the given available width. This will use
    /// the formatter defined for the value to render it.
    protected void extractValueLabel(GuiGraphicsExtractor graphics, int rightOffset, int availableWidth) {
        this.extractValueLabel(graphics, rightOffset, availableWidth, optionProxy.option.value.formatter.apply(optionProxy.value));
    }

    /// Render a simple value label at the given offset from the right, with the given available width.
    protected void extractValueLabel(GuiGraphicsExtractor graphics, int rightOffset, int availableWidth, Component value) {
        ActiveTextCollector textCollector = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE);
        textCollector.acceptScrolling(value,
                this.getRight() - rightOffset - 2 - 4,
                this.getRight() - rightOffset - availableWidth + 2 + 4,
                this.getRight() - rightOffset - 2 - 4,
                this.getY() + 2, this.getBottom() - 2);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()),
                this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                ARGB.white(this.alpha));
        this.extractOptionWidget(graphics, mouseX, mouseY, partialTick);
        this.handleCursor(graphics);
    }

    /// Render the contents of the option label with the given parameters. The background is already rendered, and
    /// should not be drawn by this function.
    protected abstract void extractOptionWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);

    /// The super class of an option widget that has a simple action when it is clicked.
    @NullMarked
    @ApiStatus.Internal
    public static abstract class Clickable<T> extends OptionWidget<T> {
        public Clickable(OptionProxy<T> optionProxy) {
            super(optionProxy);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
            this.onPress(event);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!this.isActive()) {
                return false;
            } else if (event.isSelection()) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onPress(event);
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void extractOptionWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
            this.handleCursor(graphics);
        }

        /// Perform the operation that should happen when the widget is clicked.
        public abstract void onPress(InputWithModifiers input);
    }
}
