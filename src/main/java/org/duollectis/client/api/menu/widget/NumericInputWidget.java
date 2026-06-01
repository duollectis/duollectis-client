package org.duollectis.client.api.menu.widget;

import lombok.Setter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.COLOR_TEXT_DIM;

/**
 * Текстовое поле для ввода числовых значений с валидацией диапазона, суффиксом
 * и автоматическим применением значения при потере фокуса или нажатии Enter.
 */
public class NumericInputWidget extends TextFieldWidget {

	private static final int TEXT_PADDING = 4;

	private final TextRenderer textRenderer;
	private final double inputMin;
	private final double inputMax;
	private final boolean integerOnly;
	private final int decimalPlaces;
	private final String suffix;
	private final Consumer<Double> onCommit;
	private final int rightEdge;
	private final int maxExpandW;

	private StyledSliderWidget linkedSlider;

	@Setter
	private Runnable onBlur;
	private boolean committing;

	public NumericInputWidget(
			TextRenderer textRenderer,
			int x,
			int y,
			int w,
			int h,
			int maxExpandW,
			double inputMin,
			double inputMax,
			boolean integerOnly,
			int decimalPlaces,
			String suffix,
			String initialText,
			Consumer<Double> onCommit
	) {
		super(textRenderer, x, y, w, h, Text.empty());
		this.textRenderer = textRenderer;
		this.inputMin = inputMin;
		this.inputMax = inputMax;
		this.integerOnly = integerOnly;
		this.decimalPlaces = decimalPlaces;
		this.suffix = suffix;
		this.onCommit = onCommit;
		this.rightEdge = x + w;
		this.maxExpandW = maxExpandW;

		setDrawsBackground(false);
		setTextPredicate(this::isValidRawInput);

		double initial = parseRaw(initialText);
		setRawText(formatNumber(initial));
	}

	public void linkSlider(StyledSliderWidget slider) {
		linkedSlider = slider;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
			commitAndBlur();
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public void setFocused(boolean focused) {
		boolean wasFocused = isFocused();
		boolean gainedFocus = focused && !wasFocused;
		boolean lostFocus = wasFocused && !focused;

		if (gainedFocus) {
			width = maxExpandW;
			setX(rightEdge - maxExpandW);
			setDrawsBackground(true);
		}
		else if (lostFocus) {
			setDrawsBackground(false);
			collapseToText(getText());
		}

		super.setFocused(focused);

		if (gainedFocus) {
			setRawText(stripSuffix(getText()));
		}
		else if (lostFocus && !committing) {
			applyCommit();
		}
	}

	@Override
	public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
		if (isFocused()) {
			super.renderWidget(ctx, mx, my, delta);
			return;
		}

		String display = getText();
		int textX = rightEdge - textRenderer.getWidth(display) - TEXT_PADDING;
		ctx.drawTextWithShadow(textRenderer, Text.literal(display), textX, getY(), COLOR_TEXT_DIM);
	}

	@Override
	public void setText(String text) {
		if (isFocused()) {
			super.setText(text);
			return;
		}

		collapseToText(text);
		super.setText(text);
	}

	public void updateDisplay(double value) {
		if (isFocused()) {
			return;
		}

		setDisplayText(formatNumber(value));
	}

	private void collapseToText(String text) {
		int newWidth = textRenderer.getWidth(text) + TEXT_PADDING * 2;
		width = newWidth;
		setX(rightEdge - newWidth);
	}

	private void commitAndBlur() {
		committing = true;
		applyCommit();
		setFocused(false);
		committing = false;

		if (onBlur != null) {
			onBlur.run();
		}
	}

	private void applyCommit() {
		String raw = getText().trim().replace(',', '.');
		double clamped = Math.clamp(parseRaw(raw), inputMin, inputMax);

		onCommit.accept(clamped);

		if (linkedSlider != null) {
			linkedSlider.setValue(clamped);
		}

		setDisplayText(formatNumber(clamped));
	}

	private void setRawText(String text) {
		setUnvalidatedText(text);
	}

	private void setDisplayText(String text) {
		setUnvalidatedText(text);
	}

	private void setUnvalidatedText(String text) {
		setTextPredicate(s -> true);
		setText(text);
		setTextPredicate(this::isValidRawInput);
	}

	private boolean isValidRawInput(String text) {
		if (text.isEmpty()) {
			return true;
		}

		if (text.equals("-")) {
			return inputMin < 0;
		}

		String normalized = text.replace(',', '.');

		return integerOnly
			? normalized.matches("-?\\d+")
			: normalized.matches("-?\\d*\\.?\\d*");
	}

	private String stripSuffix(String text) {
		if (text == null) {
			return "";
		}

		if (suffix.isEmpty()) {
			return text;
		}

		String withSpace = " " + suffix;

		return text.endsWith(withSpace)
			? text.substring(0, text.length() - withSpace.length())
			: text;
	}

	private double parseRaw(String raw) {
		if (raw == null || raw.isBlank() || raw.equals("-")) {
			return inputMin;
		}

		try {
			return Double.parseDouble(raw.trim().replace(',', '.'));
		}
		catch (NumberFormatException ignored) {
			return inputMin;
		}
	}

	private String formatNumber(double value) {
		String number = integerOnly
				? String.valueOf((int) Math.round(value))
				: String.format("%." + decimalPlaces + "f", value);

		return suffix.isEmpty() ? number : number + " " + suffix;
	}
}
