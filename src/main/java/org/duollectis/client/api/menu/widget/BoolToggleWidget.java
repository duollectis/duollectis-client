package org.duollectis.client.api.menu.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;
import static org.duollectis.client.api.menu.widget.WidgetAnimUtils.lerpColor;
import static org.duollectis.client.api.menu.widget.WidgetAnimUtils.smoothstep;

/**
 * Анимированный переключатель (toggle) для булевых настроек.
 * Плавно интерполирует цвет трека и позицию ползунка через {@link WidgetAnimUtils}.
 */
public class BoolToggleWidget extends ClickableWidget {

	private static final int TOGGLE_W = 28;
	private static final int TOGGLE_H = 10;
	private static final int KNOB_SIZE = 6;
	private static final int KNOB_MARGIN = 2;
	private static final float ANIM_DECAY = 0.08f;

	private final TextRenderer textRenderer;
	private final String label;
	private final Consumer<Boolean> onChange;
	private boolean value;
	private float animPos;

	public BoolToggleWidget(
			TextRenderer textRenderer,
			int x,
			int y,
			int w,
			int h,
			String label,
			boolean initialValue,
			Consumer<Boolean> onChange
	) {
		super(x, y, w, h, Text.empty());
		this.textRenderer = textRenderer;
		this.label = label;
		this.value = initialValue;
		this.animPos = initialValue ? 1f : 0f;
		this.onChange = onChange;
	}

	@Override
	protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
		float target = value ? 1f : 0f;
		animPos += (target - animPos) * (1f - (float) Math.pow(ANIM_DECAY, delta));

		float eased = smoothstep(animPos);

		int toggleX = getX() + getWidth() - TOGGLE_W;
		int toggleY = getY() + (getHeight() - TOGGLE_H) / 2;

		int nameColor = lerpColor(COLOR_TEXT_DIM, COLOR_TEXT_ACTIVE, eased);
		ctx.drawTextWithShadow(
			textRenderer,
			Text.literal(label),
			getX(),
			getY() + (getHeight() - 8) / 2,
			nameColor
		);

		renderTrack(ctx, toggleX, toggleY, eased);
		renderKnob(ctx, toggleX, toggleY, eased);
	}

	private void renderTrack(DrawContext ctx, int toggleX, int toggleY, float eased) {
		boolean hovered = isHovered();
		int offColor = hovered ? COLOR_BTN_HOVER : COLOR_BTN_BG;
		int onColor = hovered ? COLOR_BTN_ACTIVE_HOV : COLOR_BTN_ACTIVE;
		int trackColor = lerpColor(offColor, onColor, eased);

		ctx.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, trackColor);
		drawBorder(ctx, toggleX, toggleY, TOGGLE_W, TOGGLE_H, COLOR_BTN_BORDER);
	}

	private void renderKnob(DrawContext ctx, int toggleX, int toggleY, float eased) {
		int travelRange = TOGGLE_W - KNOB_SIZE - KNOB_MARGIN * 2;
		int knobX = toggleX + KNOB_MARGIN + Math.round(eased * travelRange);
		int knobY = toggleY + (TOGGLE_H - KNOB_SIZE) / 2;
		int knobColor = lerpColor(COLOR_BTN_TEXT, COLOR_BTN_TEXT_ON, eased);

		ctx.fill(knobX, knobY, knobX + KNOB_SIZE, knobY + KNOB_SIZE, knobColor);
	}

	public boolean handleClick(double mx, double my) {
		if (mx < getX()
			|| mx >= getX() + getWidth()
			|| my < getY()
			|| my >= getY() + getHeight()
		) {
			return false;
		}

		value = !value;
		onChange.accept(value);

		return true;
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
	}
}
