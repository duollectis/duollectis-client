package org.duollectis.client.api.menu.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class StyledSliderWidget extends ClickableWidget {

    private static final int TRACK_H = 4;
    private static final int THUMB_W = 8;
    private static final int THUMB_H = 14;
    private static final int COLOR_TRACK = 0xFF2A2A2A;
    private static final int COLOR_FILL = 0xFF4466CC;
    private static final int COLOR_THUMB = 0xFF88AAFF;
    private static final int COLOR_THUMB_DRAG = 0xFFAABBFF;
    private static final int COLOR_BORDER = 0xFF334466;

    private final TextRenderer textRenderer;
    private final double min;
    private final double max;
    private final int decimalPlaces;
    private Consumer<Double> onChange;

    private double normalizedValue;
    private boolean dragging = false;

    public StyledSliderWidget(
            TextRenderer textRenderer,
            int x,
            int y,
            int w,
            int h,
            double min,
            double max,
            double initialValue,
            int decimalPlaces,
            Consumer<Double> onChange
    ) {
        super(x, y, w, h, Text.empty());
        this.textRenderer = textRenderer;
        this.min = min;
        this.max = max;
        this.decimalPlaces = decimalPlaces;
        this.onChange = onChange;
        this.normalizedValue = toNormalized(initialValue);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        int cx = getX();
        int cy = getY();
        int cw = getWidth();
        int ch = getHeight();

        int trackY = cy + (ch - TRACK_H) / 2;
        int thumbX = cx + (int) (normalizedValue * (cw - THUMB_W));
        int thumbY = cy + (ch - THUMB_H) / 2;

        ctx.fill(cx, trackY, cx + cw, trackY + TRACK_H, COLOR_TRACK);
        drawBorder(ctx, cx, trackY, cw, TRACK_H, COLOR_BORDER);

        ctx.fill(cx, trackY, thumbX + THUMB_W / 2, trackY + TRACK_H, COLOR_FILL);

        boolean thumbHovered = mx >= thumbX && mx < thumbX + THUMB_W
            && my >= thumbY && my < thumbY + THUMB_H;
        int thumbColor = (dragging || thumbHovered) ? COLOR_THUMB_DRAG : COLOR_THUMB;
        ctx.fill(thumbX, thumbY, thumbX + THUMB_W, thumbY + THUMB_H, thumbColor);
        drawBorder(ctx, thumbX, thumbY, THUMB_W, THUMB_H, COLOR_BORDER);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    public boolean handleClick(double mx, double my) {
        if (mx < getX() || mx >= getX() + getWidth() || my < getY() || my >= getY() + getHeight()) {
            return false;
        }

        dragging = true;
        applyMouseX(mx);
        return true;
    }

    public boolean handleDrag(double mx) {
        if (dragging == false) {
            return false;
        }

        applyMouseX(mx);
        return true;
    }

    public void handleRelease() {
        dragging = false;
    }

    public double mappedValue() {
        return min + normalizedValue * (max - min);
    }

    public void setValue(double value) {
        normalizedValue = toNormalized(value);
    }

    public void setOnChange(Consumer<Double> onChange) {
        this.onChange = onChange;
    }

    private void applyMouseX(double mx) {
        double usableWidth = getWidth() - THUMB_W;
        double relative = mx - getX() - THUMB_W / 2.0;
        normalizedValue = Math.clamp(relative / usableWidth, 0.0, 1.0);

        if (onChange != null) {
            onChange.accept(mappedValue());
        }
    }

    private double toNormalized(double value) {
        if (max <= min) {
            return 0.0;
        }

        return Math.clamp((value - min) / (max - min), 0.0, 1.0);
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

}
