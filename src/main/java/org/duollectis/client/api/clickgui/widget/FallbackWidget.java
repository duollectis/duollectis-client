package org.duollectis.client.api.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.settings.Setting;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/** Виджет-заглушка для неизвестных типов настроек: имя слева, значение toString() справа. */
public final class FallbackWidget implements ClickGuiSettingWidget {

	private final Setting<?> setting;

	public FallbackWidget(Setting<?> setting) {
		this.setting = setting;
	}

	@Override
	public int height() {
		return SETTING_INLINE_H;
	}

	@Override
	public void render(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + height();
		ctx.fill(x, y, x + width, y + height(), hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW);

		int maxNameW = width / 2 - PAD;
		String name = textRenderer.trimToWidth(setting.getName(), maxNameW);
		ctx.drawTextWithShadow(textRenderer, Text.literal(name), x + PAD, y + (height() - 8) / 2, COLOR_TEXT_DIM);

		String val = textRenderer.trimToWidth(setting.toString(), width / 2 - PAD);
		int valX = x + width - PAD - textRenderer.getWidth(val);
		ctx.drawTextWithShadow(textRenderer, Text.literal(val), valX, y + (height() - 8) / 2, COLOR_TEXT_VALUE);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, double mx, double my, int btn) {
		return false;
	}
}
