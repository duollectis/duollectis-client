package org.duollectis.client.api.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.settings.EnumSetting;
import org.lwjgl.glfw.GLFW;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/**
 * Виджет перечисления: имя слева, кнопки ◀ ▶ и текущее значение справа.
 * ЛКМ по правой стрелке или по значению — следующий вариант.
 * ПКМ по значению — предыдущий вариант.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class EnumWidget implements ClickGuiSettingWidget {

	private static final int ARROW_BTN_W = 10;

	private final EnumSetting setting;

	public EnumWidget(EnumSetting setting) {
		this.setting = setting;
	}

	@Override
	public int height() {
		return SETTING_INLINE_H;
	}

	@Override
	public void render(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + height();
		int rowBg = hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW;
		ctx.fill(x, y, x + width, y + height(), rowBg);

		String valueStr = setting.get().toString();
		int valueW = textRenderer.getWidth(valueStr);
		int arrowsW = ARROW_BTN_W * 2 + 2;
		int maxNameW = width - PAD * 2 - valueW - arrowsW - 4;
		String name = textRenderer.trimToWidth(setting.getName(), maxNameW);

		ctx.drawTextWithShadow(textRenderer, Text.literal(name), x + PAD, y + (height() - 8) / 2, COLOR_TEXT_DIM);

		int rightArrowX = x + width - PAD - ARROW_BTN_W;
		int leftArrowX = rightArrowX - ARROW_BTN_W - 2;
		int btnY = y + (height() - 8) / 2;

		boolean leftHov = mx >= leftArrowX && mx < leftArrowX + ARROW_BTN_W && my >= y && my < y + height();
		boolean rightHov = mx >= rightArrowX && mx < rightArrowX + ARROW_BTN_W && my >= y && my < y + height();

		int leftColor = leftHov ? COLOR_TEXT_PRIMARY : COLOR_TEXT_DIM;
		int rightColor = rightHov ? COLOR_TEXT_PRIMARY : COLOR_TEXT_DIM;

		ctx.drawTextWithShadow(textRenderer, Text.literal("◀"), leftArrowX, btnY, leftColor);
		ctx.drawTextWithShadow(textRenderer, Text.literal("▶"), rightArrowX, btnY, rightColor);

		int valX = leftArrowX - 3 - valueW;
		ctx.drawTextWithShadow(textRenderer, Text.literal(valueStr), valX, y + (height() - 8) / 2, COLOR_TEXT_VALUE);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, double mx, double my, int btn) {
		if (mx < x || mx >= x + width || my < y || my >= y + height()) {
			return false;
		}

		int rightArrowX = x + width - PAD - ARROW_BTN_W;
		int leftArrowX = rightArrowX - ARROW_BTN_W - 2;

		boolean onLeft = mx >= leftArrowX && mx < leftArrowX + ARROW_BTN_W;
		boolean onRight = mx >= rightArrowX && mx < rightArrowX + ARROW_BTN_W;

		if (btn == GLFW.GLFW_MOUSE_BUTTON_1) {
			if (onLeft) {
				cyclePrev();
			}
			else {
				cycleNext();
			}

			return true;
		}

		if (btn == GLFW.GLFW_MOUSE_BUTTON_2 && !onRight) {
			cyclePrev();
			return true;
		}

		return true;
	}

	private void cycleNext() {
		Enum<?> current = (Enum<?>) setting.get();
		Enum<?>[] values = current.getDeclaringClass().getEnumConstants();
		int next = (current.ordinal() + 1) % values.length;
		setting.set(values[next]);
	}

	private void cyclePrev() {
		Enum<?> current = (Enum<?>) setting.get();
		Enum<?>[] values = current.getDeclaringClass().getEnumConstants();
		int prev = (current.ordinal() - 1 + values.length) % values.length;
		setting.set(values[prev]);
	}
}
