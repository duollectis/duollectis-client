package org.duollectis.client.api.menu.account;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import static org.duollectis.client.api.menu.account.AccountScreenLayout.FIELD_H;

/** Вспомогательные методы экрана аккаунтов: отрисовка рамок, фабрики полей ввода. */
final class AccountScreenUtil {

	private AccountScreenUtil() {}

	static Text translatable(String key) {
		return Text.translatable(key);
	}

	static String translatableString(String key) {
		return Text.translatable(key).getString();
	}

	static void drawRect(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}

	static void drawSectionHeader(DrawContext ctx, TextRenderer tr, int x, int y, int w, String label) {
		ctx.drawTextWithShadow(tr, Text.literal(label), x, y, 0xFFAAC8F0);
		int lineY = y + AccountScreenLayout.LABEL_H + 1;
		ctx.fill(x, lineY, x + w, lineY + 1, 0xFF1E2A40);
	}

	static TextFieldWidget field(TextRenderer tr, int x, int y, int w, String placeholder) {
		TextFieldWidget f = new TextFieldWidget(tr, x, y, w, FIELD_H, Text.empty());
		f.setPlaceholder(Text.translatable(placeholder));
		return f;
	}
}
