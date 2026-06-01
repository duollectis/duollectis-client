package org.duollectis.client.api.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.settings.BoolSetting;
import org.lwjgl.glfw.GLFW;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;
import static org.duollectis.client.api.menu.widget.WidgetAnimUtils.*;

/**
 * Виджет булевой настройки: имя слева, анимированный toggle-switch справа.
 * ЛКМ — переключает значение.
 */
public final class BoolWidget implements ClickGuiSettingWidget {

	private static final int TOGGLE_W = 24;
	private static final int TOGGLE_H = 8;
	private static final int KNOB_SIZE = 4;
	private static final int KNOB_MARGIN = 2;
	private static final float ANIM_DECAY = 0.08f;

	private final BoolSetting setting;
	private float animPos;

	public BoolWidget(BoolSetting setting) {
		this.setting = setting;
		this.animPos = setting.get() ? 1f : 0f;
	}

	@Override
	public int height() {
		return SETTING_INLINE_H;
	}

	@Override
	public void render(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		boolean val = setting.get();
		float target = val ? 1f : 0f;
		animPos += (target - animPos) * (1f - (float) Math.pow(ANIM_DECAY, 1.0));

		float eased = smoothstep(animPos);
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + height();

		int rowBg = hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW;
		ctx.fill(x, y, x + width, y + height(), rowBg);

		int maxNameW = width - PAD * 2 - TOGGLE_W - 4;
		String name = textRenderer.trimToWidth(setting.getName(), maxNameW);
		int nameColor = lerpColor(COLOR_TEXT_DIM, COLOR_TEXT_PRIMARY, eased);
		ctx.drawTextWithShadow(textRenderer, Text.literal(name), x + PAD, y + (height() - 8) / 2, nameColor);

		renderToggle(ctx, x, y, width, eased, hovered);
	}

	private void renderToggle(DrawContext ctx, int x, int y, int width, float eased, boolean hovered) {
		int toggleX = x + width - PAD - TOGGLE_W;
		int toggleY = y + (height() - TOGGLE_H) / 2;

		int offColor = hovered ? 0xFF1A2838 : COLOR_BTN_BG;
		int onColor = hovered ? 0xFF1A4A2A : 0xFF0F3020;
		int trackColor = lerpColor(offColor, onColor, eased);

		ctx.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, trackColor);
		drawBorder(ctx, toggleX, toggleY, TOGGLE_W, TOGGLE_H, COLOR_BTN_BORDER);

		int travelRange = TOGGLE_W - KNOB_SIZE - KNOB_MARGIN * 2;
		int knobX = toggleX + KNOB_MARGIN + Math.round(eased * travelRange);
		int knobY = toggleY + (TOGGLE_H - KNOB_SIZE) / 2;
		int knobColor = lerpColor(COLOR_TEXT_DIM, COLOR_TEXT_ACTIVE, eased);

		ctx.fill(knobX, knobY, knobX + KNOB_SIZE, knobY + KNOB_SIZE, knobColor);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, double mx, double my, int btn) {
		if (btn != GLFW.GLFW_MOUSE_BUTTON_1) {
			return false;
		}

		if (mx < x || mx >= x + width || my < y || my >= y + height()) {
			return false;
		}

		setting.set(!setting.get());
		return true;
	}

	private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}

	private static final int COLOR_BTN_BG = 0xFF101820;
}
