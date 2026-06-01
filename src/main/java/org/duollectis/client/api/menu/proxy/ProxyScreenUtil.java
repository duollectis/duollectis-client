package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.FIELD_H;
import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.LABEL_H;
import static org.duollectis.client.utils.MC.translations;

/**
 * Вспомогательные методы экрана прокси: отрисовка рамок, заголовков секций,
 * парсинг чисел и фабрики текстовых полей.
 */
final class ProxyScreenUtil {

	static final int ENTRY_H = 22;

	private ProxyScreenUtil() {}

	static Text translatable(String key) {
		return Text.literal(translations().get(key));
	}

	static String translatableString(String key) {
		return translations().get(key);
	}

	static String tsa(String key, Object... args) {
		return String.format(translations().get(key), args);
	}

	static void drawRect(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}

	static void drawSectionHeader(DrawContext ctx, TextRenderer tr, int x, int y, int w, String label) {
		ctx.drawTextWithShadow(tr, Text.literal(label), x, y, 0xFFAAC8F0);
		int lineY = y + LABEL_H + 1;
		ctx.fill(x, lineY, x + w, lineY + 1, 0xFF1E2A40);
	}

	static int parseIntOr(String s, int fallback) {
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	static TextFieldWidget field(TextRenderer tr, int x, int y, int w, String placeholder) {
		TextFieldWidget f = new TextFieldWidget(tr, x, y, w, FIELD_H, Text.empty());
		f.setPlaceholder(Text.translatable(placeholder));
		return f;
	}

	static TextFieldWidget numericField(TextRenderer tr, int x, int y, int w, int maxLen, String initialValue, String digitRegex) {
		TextFieldWidget f = new TextFieldWidget(tr, x, y, w, FIELD_H, Text.empty());
		f.setMaxLength(maxLen);
		f.setText(initialValue);
		f.setTextPredicate(v -> v.isEmpty() || v.matches(digitRegex));
		return f;
	}
}
