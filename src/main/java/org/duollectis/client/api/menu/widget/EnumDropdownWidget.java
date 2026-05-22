package org.duollectis.client.api.menu.widget;

import lombok.Getter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.duollectis.client.utils.INamed;

import java.util.function.Consumer;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.drawBorder;

/**
 * Выпадающий список для выбора значения перечисления. Поддерживает интерфейс
 * {@link INamed} для кастомных отображаемых имён и рендерит оверлей поверх других виджетов.
 */
public class EnumDropdownWidget<E extends Enum<E>> extends ClickableWidget {

	private static final int ITEM_H = 20;

	private final TextRenderer textRenderer;
	private final E[] values;

	@Getter
	private E selected;

	@Getter
	private boolean open;

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
		int bg = hovered ? 0xFF1E2E48 : 0xFF141C2C;
		ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
		drawBorder(
				ctx, getX(), getY(), getWidth(), getHeight(),
				open ? 0xFF5599CC : 0xFF2A3A5A
		);
		ctx.drawTextWithShadow(
				textRenderer,
				Text.literal(getName(selected) + (open ? " ▲" : " ▼")),
				getX() + 4, getY() + (getHeight() - 8) / 2, 0xFFCDD5E0
		);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

	public void renderOverlay(DrawContext ctx, int mx, int my) {
		if (open) {
			renderDropdownItems(ctx, mx, my);
		}
	}

	public boolean handleClick(double mx, double my) {
		boolean overHeader = mx >= getX()
				&& mx < getX() + getWidth()
				&& my >= getY()
				&& my < getY() + getHeight();

		if (overHeader) {
			open = !open;
			return true;
		}

		if (open) {
			for (int i = 0; i < values.length; i++) {
				int iy = getY() + getHeight() + i * ITEM_H;
				boolean overItem = mx >= getX()
						&& mx < getX() + getWidth()
						&& my >= iy
						&& my < iy + ITEM_H;

				if (overItem) {
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

	private void renderDropdownItems(DrawContext ctx, int mx, int my) {
		for (int i = 0; i < values.length; i++) {
			int iy = getY() + getHeight() + i * ITEM_H;
			boolean isSel = values[i] == selected;
			boolean isHovered = mx >= getX()
					&& mx < getX() + getWidth()
					&& my >= iy
					&& my < iy + ITEM_H;

			int bg = isSel ? 0xFF182440
			               : isHovered ? 0xFF1E2E48
			                           : 0xFF0C0F15;

			ctx.fill(getX(), iy, getX() + getWidth(), iy + ITEM_H, bg);
			drawBorder(ctx, getX(), iy, getWidth(), ITEM_H, 0xFF2A3A5A);
			ctx.drawTextWithShadow(
					textRenderer,
					Text.literal((isSel ? "● " : "  ") + getName(values[i])),
					getX() + 4, iy + (ITEM_H - 8) / 2,
					isSel ? 0xFF77BBEE : 0xFFBBCCDD
			);
		}
	}

}
