package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.FIELD_H;
import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.LABEL_H;

/** Shared utility methods used across the Proxy Manager screen classes. */
final class ProxyScreenUtil {
    private ProxyScreenUtil() {}

    /** Entry row height in pixels — shared layout constant. */
    static final int ENTRY_H = 22;

    /** Returns a {@link Text} for the given translation key. */
    static Text t(String key) {
        return Text.translatable(key);
    }

    /** Returns the plain string for the given translation key. */
    static String ts(String key) {
        return Text.translatable(key).getString();
    }

    /** Returns a formatted string using the translation key as a format template. */
    static String tsa(String key, Object... args) {
        return String.format(Text.translatable(key).getString(), args);
    }

    /** Draws a 1-pixel border rectangle. */
    static void drawRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    /** Parses an integer from {@code s}, returning {@code fallback} on failure. */
    static int parseIntOr(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    /**
     * Creates a {@link TextFieldWidget} with the given placeholder translation key.
     * The widget is NOT added to the screen — caller must call {@code addDrawableChild}.
     */
    static TextFieldWidget field(TextRenderer tr, int x, int y, int w, String placeholderKey) {
        TextFieldWidget f = new TextFieldWidget(tr, x, y, w, FIELD_H, Text.empty());
        f.setPlaceholder(t(placeholderKey));
        return f;
    }

    /** Draws a section header: coloured label + thin divider line below it. */
    static void drawSectionHeader(DrawContext ctx, TextRenderer tr, int x, int y, int w, String label) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, 0xFF88AACC);
        int lineY = y + LABEL_H + 1;
        ctx.fill(x, lineY, x + w, lineY + 1, 0xFF334455);
    }
}
