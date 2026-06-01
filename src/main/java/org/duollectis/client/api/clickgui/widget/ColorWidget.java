package org.duollectis.client.api.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.settings.ColorSetting;
import org.duollectis.client.utils.color.Color;
import org.lwjgl.glfw.GLFW;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/**
 * Виджет цветовой настройки.
 * Первая строка: имя + квадрат превью цвета.
 * Следующие 4 строки: слайдеры R, G, B, A с заливкой соответствующим цветом.
 * ЛКМ по слайдеру — перетаскивание значения канала.
 */
public final class ColorWidget implements ClickGuiSettingWidget {

	private static final int PREVIEW_SIZE = 10;
	private static final int CHANNEL_ROW_H = 12;
	private static final int HEADER_ROW_H = SETTING_INLINE_H;
	private static final int TOTAL_H = HEADER_ROW_H + CHANNEL_ROW_H * 4;

	/** Индекс активно перетаскиваемого канала (0=R,1=G,2=B,3=A), -1 если нет. */
	private int draggingChannel = -1;

	private final ColorSetting setting;

	public ColorWidget(ColorSetting setting) {
		this.setting = setting;
	}

	@Override
	public int height() {
		return TOTAL_H;
	}

	@Override
	public void render(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + HEADER_ROW_H;
		int rowBg = hovered ? COLOR_SETTING_ROW_HOVER : COLOR_SETTING_ROW;
		ctx.fill(x, y, x + width, y + HEADER_ROW_H, rowBg);

		String name = textRenderer.trimToWidth(setting.getName(), width - PAD * 2 - PREVIEW_SIZE - 4);
		ctx.drawTextWithShadow(textRenderer, Text.literal(name), x + PAD, y + (HEADER_ROW_H - 8) / 2, COLOR_TEXT_DIM);

		Color color = setting.get();
		int previewX = x + width - PAD - PREVIEW_SIZE;
		int previewY = y + (HEADER_ROW_H - PREVIEW_SIZE) / 2;
		ctx.fill(previewX, previewY, previewX + PREVIEW_SIZE, previewY + PREVIEW_SIZE, color.getPacked());
		drawBorder(ctx, previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE, 0xFF334455);

		renderChannelSlider(ctx, textRenderer, x, y + HEADER_ROW_H, width, mx, my, 0, "R", color.r, 0xFFCC2222);
		renderChannelSlider(ctx, textRenderer, x, y + HEADER_ROW_H + CHANNEL_ROW_H, width, mx, my, 1, "G", color.g, 0xFF22CC44);
		renderChannelSlider(ctx, textRenderer, x, y + HEADER_ROW_H + CHANNEL_ROW_H * 2, width, mx, my, 2, "B", color.b, 0xFF2266CC);
		renderChannelSlider(ctx, textRenderer, x, y + HEADER_ROW_H + CHANNEL_ROW_H * 3, width, mx, my, 3, "A", color.a, 0xFF888888);
	}

	private void renderChannelSlider(
			DrawContext ctx,
			TextRenderer textRenderer,
			int x,
			int y,
			int width,
			int mx,
			int my,
			int channel,
			String label,
			int channelValue,
			int fillColor
	) {
		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + CHANNEL_ROW_H;
		ctx.fill(x, y, x + width, y + CHANNEL_ROW_H, COLOR_SETTING_ROW);

		double ratio = channelValue / 255.0;
		int fillW = (int) (width * ratio);

		int actualFill = hovered ? blendHover(fillColor) : fillColor;

		if (fillW > 0) {
			ctx.fill(x, y, x + fillW, y + CHANNEL_ROW_H, actualFill);
		}

		if (fillW > 0 && fillW < width) {
			ctx.fill(x + fillW, y, x + fillW + 1, y + CHANNEL_ROW_H, 0xFF1E4060);
		}

		String valueStr = String.valueOf(channelValue);
		int valueW = textRenderer.getWidth(valueStr);
		int labelW = textRenderer.getWidth(label);

		ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + PAD, y + (CHANNEL_ROW_H - 8) / 2, 0xFFCDD5E0);

		int valX = x + width - PAD - valueW;
		ctx.drawTextWithShadow(textRenderer, Text.literal(valueStr), valX, y + (CHANNEL_ROW_H - 8) / 2, COLOR_TEXT_VALUE);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, double mx, double my, int btn) {
		if (mx < x || mx >= x + width || my < y || my >= y + height()) {
			return false;
		}

		if (btn != GLFW.GLFW_MOUSE_BUTTON_1) {
			return true;
		}

		int channelIndex = channelAt(y, my);

		if (channelIndex < 0) {
			return true;
		}

		draggingChannel = channelIndex;
		applyChannelDrag(x, width, mx);
		return true;
	}

	@Override
	public boolean mouseDragged(int x, int y, int width, double mx) {
		if (draggingChannel < 0) {
			return false;
		}

		applyChannelDrag(x, width, mx);
		return true;
	}

	@Override
	public void mouseReleased() {
		draggingChannel = -1;
	}

	private void applyChannelDrag(int panelX, int panelW, double mx) {
		int value = (int) Math.round(Math.clamp((mx - panelX) / panelW, 0.0, 1.0) * 255);
		Color color = setting.get();

		Color updated = switch (draggingChannel) {
			case 0 -> new Color(value, color.g, color.b, color.a);
			case 1 -> new Color(color.r, value, color.b, color.a);
			case 2 -> new Color(color.r, color.g, value, color.a);
			case 3 -> new Color(color.r, color.g, color.b, value);
			default -> color;
		};

		setting.set(updated);
	}

	/** Возвращает индекс канала (0-3) по Y-позиции мыши, или -1 если не попал. */
	private int channelAt(int panelY, double my) {
		int channelsStart = panelY + HEADER_ROW_H;

		if (my < channelsStart) {
			return -1;
		}

		int index = (int) ((my - channelsStart) / CHANNEL_ROW_H);
		return index >= 0 && index < 4 ? index : -1;
	}

	/** Осветляет цвет для состояния hover. */
	private static int blendHover(int color) {
		int a = (color >> 24) & 0xFF;
		int r = Math.min(255, ((color >> 16) & 0xFF) + 20);
		int g = Math.min(255, ((color >> 8) & 0xFF) + 20);
		int b = Math.min(255, (color & 0xFF) + 20);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
