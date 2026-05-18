package org.duollectis.client.api.menu.widget;

import lombok.Getter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * A generic enum dropdown widget.
 * Usage:
 * EnumDropdownWidget<MyEnum> dd = new EnumDropdownWidget<>(
 * textRenderer, x, y, w, h, MyEnum.values(), MyEnum.DEFAULT, this::onChanged);
 * addDrawableChild(dd);
 * IMPORTANT: call {@link #renderOverlay(DrawContext, int, int)} from the screen's
 * render() AFTER super.render() so the expanded list draws on top of other widgets.
 * Also call {@link #handleClick(double, double)} from the screen's mouseClicked()
 * BEFORE super.mouseClicked() to handle header toggle and item selection.
 */
public class EnumDropdownWidget<E extends Enum<E>> extends ClickableWidget {

	private static final int ITEM_H = 20;

	private final TextRenderer textRenderer;
	private final E[] values;
	// ── getters ───────────────────────────────────────────────────────────────

	@Getter
	private E selected;

	@Getter
	private boolean open = false;
	private final Consumer<E> onChange;

	public EnumDropdownWidget(
			TextRenderer textRenderer,
			int x, int y, int w, int h,
			E[] values, E initial,
			Consumer<E> onChange
	) {
		super(x, y, w, h, Text.literal(initial.name()));
		this.textRenderer = textRenderer;
		this.values = values;
		this.selected = initial;
		this.onChange = onChange;
	}

	// ── ClickableWidget contract ──────────────────────────────────────────────

	/**
	 * Draws the collapsed header.
	 */
	@Override
	protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
		boolean hovered = isHovered();
		int bg = hovered ? 0xFF555555 : 0xFF333333;
		ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
		drawBorder(
				ctx, getX(), getY(), getWidth(), getHeight(),
				open ? 0xFF88AAFF : 0xFF666666
		);
		ctx.drawTextWithShadow(
				textRenderer,
				Text.literal(selected.name() + (open ? " ▲" : " ▼")),
				getX() + 4, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF
		);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		// no narration needed for this widget
	}

	// ── overlay (call from screen after super.render) ─────────────────────────

	/**
	 * Draw the expanded dropdown list. Call this AFTER super.render() in the
	 * parent screen so it renders on top of all other widgets.
	 */
	public void renderOverlay(DrawContext ctx, int mx, int my) {
		if (!open)
			return;

		for (int i = 0; i < values.length; i++) {
			int iy = getY() + getHeight() + i * ITEM_H;
			boolean isSel = values[i] == selected;
			boolean isHovered = mx >= getX() && mx < getX() + getWidth()
					&& my >= iy && my < iy + ITEM_H;

			int bg = isSel ? 0xFF224488
			               : isHovered ? 0xFF444466
			                           : 0xFF222233;
			ctx.fill(getX(), iy, getX() + getWidth(), iy + ITEM_H, bg);
			drawBorder(ctx, getX(), iy, getWidth(), ITEM_H, 0xFF555577);
			ctx.drawTextWithShadow(
					textRenderer,
					Text.literal((isSel ? "● " : "  ") + values[i].name()),
					getX() + 4, iy + (ITEM_H - 8) / 2,
					isSel ? 0xFF88AAFF : 0xFFCCCCCC
			);
		}
	}

	// ── click handling (call from screen mouseClicked before super) ───────────

	/**
	 * Handle a click. Returns true if the click was consumed.
	 * Call this from the parent screen's mouseClicked() BEFORE super.mouseClicked().
	 */
	public boolean handleClick(double mx, double my) {
		// click on header → toggle open/closed
		if (mx >= getX() && mx < getX() + getWidth()
				&& my >= getY() && my < getY() + getHeight()) {
			open = !open;
			return true;
		}

		// click on expanded item
		if (open) {
			for (int i = 0; i < values.length; i++) {
				int iy = getY() + getHeight() + i * ITEM_H;
				if (mx >= getX() && mx < getX() + getWidth()
						&& my >= iy && my < iy + ITEM_H) {
					selected = values[i];
					setMessage(Text.literal(selected.name()));
					open = false;
					if (onChange != null)
						onChange.accept(selected);
					return true;
				}
			}
			// click outside expanded area → close
			open = false;
		}
		return false;
	}

	// ── helpers ───────────────────────────────────────────────────────────────
	private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
