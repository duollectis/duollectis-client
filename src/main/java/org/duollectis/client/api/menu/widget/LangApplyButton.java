package org.duollectis.client.api.menu.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

public class LangApplyButton extends ClickableWidget {

    private static final String LABEL = "Apply";

    private final TextRenderer textRenderer;
    private final Runnable onApply;

    public LangApplyButton(TextRenderer textRenderer, int x, int y, int w, int h, Runnable onApply) {
        super(x, y, w, h, Text.empty());
        this.textRenderer = textRenderer;
        this.onApply = onApply;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        int bg = isHovered() ? COLOR_BTN_HOVER : COLOR_BTN_BG;
        ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        drawBorder(ctx, getX(), getY(), getWidth(), getHeight(), COLOR_BTN_BORDER);

        int textX = getX() + (getWidth() - textRenderer.getWidth(LABEL)) / 2;
        int textY = getY() + (getHeight() - 8) / 2;
        ctx.drawTextWithShadow(textRenderer, Text.literal(LABEL), textX, textY, COLOR_BTN_TEXT);
    }

    public boolean handleClick(double mx, double my) {
        if (mx < getX()
            || mx >= getX() + getWidth()
            || my < getY()
            || my >= getY() + getHeight()
        ) {
            return false;
        }

        onApply.run();
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
