package org.duollectis.client.api.menu.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

public class StyledToggleButton extends ClickableWidget {

    private final TextRenderer textRenderer;
    private final Consumer<Boolean> onChange;
    private boolean value;

    public StyledToggleButton(
            TextRenderer textRenderer,
            int x,
            int y,
            int w,
            int h,
            boolean initialValue,
            Consumer<Boolean> onChange
    ) {
        super(x, y, w, h, Text.empty());
        this.textRenderer = textRenderer;
        this.value = initialValue;
        this.onChange = onChange;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        boolean hovered = isHovered();
        int bg = value
            ? (hovered ? COLOR_BTN_ACTIVE_HOV : COLOR_BTN_ACTIVE)
            : (hovered ? COLOR_BTN_HOVER : COLOR_BTN_BG);

        ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        drawBorder(ctx, getX(), getY(), getWidth(), getHeight(), COLOR_BTN_BORDER);

        String label = value ? "ON" : "OFF";
        int textColor = value ? COLOR_BTN_TEXT_ON : COLOR_BTN_TEXT;
        int textX = getX() + (getWidth() - textRenderer.getWidth(label)) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), textX, textY, textColor);
    }

    public boolean handleClick(double mx, double my) {
        if (mx < getX()
            || mx >= getX() + getWidth()
            || my < getY()
            || my >= getY() + getHeight()
        ) {
            return false;
        }

        value = value == false;

        if (onChange == null) {
            return true;
        }

        onChange.accept(value);
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
