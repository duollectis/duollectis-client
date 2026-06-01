package org.duollectis.client.api.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.api.settings.Setting;
import org.lwjgl.glfw.GLFW;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/**
 * Виджет числовой настройки с прогресс-баром.
 * ЛКМ — перетаскивание значения по позиции мыши.
 * ПКМ — открывает текстовое поле для ручного ввода.
 * Поддерживает {@link IntSetting} и {@link DoubleSetting}.
 */
public final class SliderWidget implements ClickGuiSettingWidget {

	private final Setting<?> setting;

	/** Активно ли перетаскивание ЛКМ. */
	private boolean dragging;

	/** Активен ли режим ручного ввода (ПКМ). */
	private boolean editing;

	/** Текстовое поле для ручного ввода (создаётся при ПКМ). */
	private TextFieldWidget editField;

	/** Рендерер шрифта, сохраняется при первом рендере для создания поля. */
	private TextRenderer cachedTextRenderer;

	public SliderWidget(Setting<?> setting) {
		this.setting = setting;
	}

	@Override
	public int height() {
		return SETTING_SLIDER_H;
	}

	public boolean isEditing() {
		return editing;
	}

	public TextFieldWidget getEditField() {
		return editField;
	}

	/** Открывает режим ручного ввода, создавая текстовое поле. */
	public void openEdit(int x, int y, int width) {
		if (cachedTextRenderer == null) {
			return;
		}

		int fieldX = x + PAD;
		int fieldW = width - PAD * 2;
		int fieldH = height() - 2;

		editField = new TextFieldWidget(cachedTextRenderer, fieldX, y + 1, fieldW, fieldH, Text.empty());
		editField.setText(formatValue());
		editField.setDrawsBackground(false);
		editField.setFocused(true);
		editing = true;
	}

	/** Применяет введённое значение и закрывает режим ввода. */
	public void applyEdit() {
		if (editField == null) {
			return;
		}

		String text = editField.getText().trim();
		applyTextValue(text);
		closeEdit();
	}

	/** Закрывает режим ввода без применения. */
	public void closeEdit() {
		editing = false;
		editField = null;
	}

	@Override
	public void render(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my) {
		cachedTextRenderer = textRenderer;

		if (editing && editField != null) {
			ctx.fill(x, y, x + width, y + height(), COLOR_SLIDER_EDIT_BG);
			drawBorder(ctx, x, y, width, height(), COLOR_EXPANDED_ARROW);
			editField.setX(x + PAD);
			editField.setY(y + 1);
			editField.setWidth(width - PAD * 2);
			editField.render(ctx, mx, my, 0);
			return;
		}

		boolean hovered = mx >= x && mx < x + width && my >= y && my < y + height();
		double ratio = computeRatio();
		int fillW = (int) (width * ratio);

		ctx.fill(x, y, x + width, y + height(), COLOR_SETTING_ROW);

		int fillColor = hovered ? COLOR_SLIDER_FILL_HOVER : COLOR_SLIDER_FILL;

		if (fillW > 0) {
			ctx.fill(x, y, x + fillW, y + height(), fillColor);
		}

		if (fillW > 0 && fillW < width) {
			ctx.fill(x + fillW, y, x + fillW + 1, y + height(), 0xFF1E4060);
		}

		String valueStr = formatValue();
		int valueW = textRenderer.getWidth(valueStr);
		int maxNameW = width - PAD * 2 - valueW - 4;
		String name = textRenderer.trimToWidth(setting.getName(), maxNameW);

		ctx.drawTextWithShadow(textRenderer, Text.literal(name), x + PAD, y + (height() - 8) / 2, COLOR_TEXT_PRIMARY);

		int valX = x + width - PAD - valueW;
		ctx.drawTextWithShadow(textRenderer, Text.literal(valueStr), valX, y + (height() - 8) / 2, COLOR_TEXT_VALUE);
	}

	@Override
	public boolean mouseClicked(int x, int y, int width, double mx, double my, int btn) {
		if (editing) {
			if (editField != null && editField.isMouseOver(mx, my)) {
				return false;
			}

			applyEdit();
			return false;
		}

		if (mx < x || mx >= x + width || my < y || my >= y + height()) {
			return false;
		}

		if (btn == GLFW.GLFW_MOUSE_BUTTON_1) {
			dragging = true;
			applyDragValue(x, width, mx);
			return true;
		}

		if (btn == GLFW.GLFW_MOUSE_BUTTON_2) {
			openEdit(x, y, width);
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(int x, int y, int width, double mx) {
		if (!dragging) {
			return false;
		}

		applyDragValue(x, width, mx);
		return true;
	}

	@Override
	public void mouseReleased() {
		dragging = false;
	}

	/** Обрабатывает нажатие клавиши в режиме ввода. */
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		if (!editing || editField == null) {
			return false;
		}

		if (input.isEnter()) {
			applyEdit();
			return true;
		}

		if (input.isEscape()) {
			closeEdit();
			return true;
		}

		editField.keyPressed(input);
		return true;
	}

	/** Обрабатывает ввод символа в режиме ввода. */
	public boolean charTyped(net.minecraft.client.input.CharInput input) {
		if (!editing || editField == null) {
			return false;
		}

		editField.charTyped(input);
		return true;
	}

	private void applyDragValue(int panelX, int panelW, double mx) {
		double ratio = Math.clamp((mx - panelX) / panelW, 0.0, 1.0);

		if (setting instanceof IntSetting intSetting) {
			int min = intSetting.getSliderMin();
			int max = intSetting.getSliderMax();
			intSetting.set((int) Math.round(min + ratio * (max - min)));
			return;
		}

		if (setting instanceof DoubleSetting doubleSetting) {
			double min = doubleSetting.getSliderMin();
			double max = doubleSetting.getSliderMax();
			doubleSetting.set(min + ratio * (max - min));
		}
	}

	private void applyTextValue(String text) {
		if (setting instanceof IntSetting intSetting) {
			try {
				intSetting.set(Integer.parseInt(text));
			}
			catch (NumberFormatException ignored) {}

			return;
		}

		if (setting instanceof DoubleSetting doubleSetting) {
			try {
				doubleSetting.set(Double.parseDouble(text));
			}
			catch (NumberFormatException ignored) {}
		}
	}

	private double computeRatio() {
		if (setting instanceof IntSetting intSetting) {
			int min = intSetting.getSliderMin();
			int max = intSetting.getSliderMax();
			int range = max - min;
			return range == 0 ? 0.0 : (double) (intSetting.get() - min) / range;
		}

		if (setting instanceof DoubleSetting doubleSetting) {
			double min = doubleSetting.getSliderMin();
			double max = doubleSetting.getSliderMax();
			double range = max - min;
			return range == 0.0 ? 0.0 : (doubleSetting.get() - min) / range;
		}

		return 0.0;
	}

	private String formatValue() {
		if (setting instanceof DoubleSetting doubleSetting) {
			return String.format("%." + doubleSetting.getDecimalPlaces() + "f", doubleSetting.get());
		}

		return setting.toString();
	}

	private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
