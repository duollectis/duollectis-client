package org.duollectis.client.api.menu.widget;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.duollectis.client.utils.INamed;

import java.util.function.Consumer;

public class EnumDropdownWidget<E extends Enum<E>> extends ClickableWidget {

	private static final int ITEM_H = 20;

	private final TextRenderer textRenderer;
	private final E[] values;

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
		super(x, y, w, h, null);
		this.textRenderer = textRenderer;
		this.values = values;
		this.selected = initial;
		this.onChange = onChange;

		setMessage(Text.literal(getName(initial)));
	}

	private String getName(E val) {
		return val instanceof INamed named
		       ? named.getName()
		       : val.name();
	}

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
				Text.literal(getName(selected) + (open ? " ▲" : " ▼")),
				getX() + 4, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF
		);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

	public void renderOverlay(DrawContext ctx, int mx, int my) {
		if (open == false) {
			return;
		}

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
					Text.literal((isSel ? "● " : "  ") + getName(values[i])),
					getX() + 4, iy + (ITEM_H - 8) / 2,
					isSel ? 0xFF88AAFF : 0xFFCCCCCC
			);
		}
	}

	public boolean handleClick(double mx, double my) {
		if (mx >= getX() && mx < getX() + getWidth()
				&& my >= getY() && my < getY() + getHeight()) {
			open = open == false;
			return true;
		}

		if (open) {
			for (int i = 0; i < values.length; i++) {
				int iy = getY() + getHeight() + i * ITEM_H;

				if (mx >= getX() && mx < getX() + getWidth()
						&& my >= iy && my < iy + ITEM_H) {
					selected = values[i];
					setMessage(Text.literal(getName(selected)));
					open = false;

					if (onChange != null) {
						onChange.accept(selected);
					}

					return true;
				}
			}

			open = false;
		}

		return false;
	}

	private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
