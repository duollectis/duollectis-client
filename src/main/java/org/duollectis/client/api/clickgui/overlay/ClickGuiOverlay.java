package org.duollectis.client.api.clickgui.overlay;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/**
 * Базовый класс всплывающего оверлея ClickGui.
 * Оверлей отображается поверх основного экрана, перехватывает весь ввод.
 * Клик вне окна оверлея или Escape — закрывает его через {@link #close()}.
 * <p>
 * Для создания нового оверлея (например, блок-листа, выбора игрока и т.д.)
 * достаточно унаследоваться и реализовать {@link #renderContent}.
 */
public abstract class ClickGuiOverlay {

	/** Ширина окна оверлея. */
	protected final int overlayW;

	/** Высота окна оверлея. */
	protected final int overlayH;

	/** Заголовок окна. */
	protected final String title;

	/** Колбэк закрытия — вызывается при Escape или клике вне окна. */
	private final Runnable onClose;

	/** X-координата левого края окна (вычисляется в {@link #init}). */
	protected int overlayX;

	/** Y-координата верхнего края окна (вычисляется в {@link #init}). */
	protected int overlayY;

	protected static final int TITLE_H = 16;

	protected ClickGuiOverlay(int overlayW, int overlayH, String title, Runnable onClose) {
		this.overlayW = overlayW;
		this.overlayH = overlayH;
		this.title = title;
		this.onClose = onClose;
	}

	/**
	 * Инициализирует оверлей: вычисляет позицию по центру экрана,
	 * создаёт дочерние виджеты.
	 */
	public void init(TextRenderer textRenderer, int screenW, int screenH) {
		overlayX = (screenW - overlayW) / 2;
		overlayY = (screenH - overlayH) / 2;
		onInit(textRenderer);
	}

	/** Вызывается после вычисления позиции — создаёт дочерние виджеты. */
	protected abstract void onInit(TextRenderer textRenderer);

	/** Рендерит оверлей: затемнение фона, рамка, заголовок, контент. */
	public final void render(DrawContext ctx, TextRenderer textRenderer, int mx, int my) {
		ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), 0x88000000);

		ctx.fill(overlayX + 3, overlayY + 3, overlayX + overlayW + 3, overlayY + overlayH + 3, 0x66000000);
		ctx.fill(overlayX, overlayY, overlayX + overlayW, overlayY + overlayH, 0xFF0D1520);
		drawBorder(ctx, overlayX, overlayY, overlayW, overlayH, COLOR_PANEL_BORDER);

		ctx.fill(overlayX, overlayY, overlayX + overlayW, overlayY + TITLE_H, COLOR_HEADER_BG);
		drawBorder(ctx, overlayX, overlayY, overlayW, TITLE_H, COLOR_PANEL_BORDER);

		int titleX = overlayX + (overlayW - textRenderer.getWidth(title)) / 2;
		int titleY = overlayY + (TITLE_H - 8) / 2;
		ctx.drawTextWithShadow(textRenderer, net.minecraft.text.Text.literal(title), titleX, titleY, COLOR_TEXT_HEADER);

		renderContent(ctx, textRenderer, mx, my);
	}

	/** Рендерит содержимое оверлея ниже заголовка. */
	protected abstract void renderContent(DrawContext ctx, TextRenderer textRenderer, int mx, int my);

	/**
	 * Обрабатывает клик мыши.
	 * Клик вне окна — закрывает оверлей.
	 *
	 * @return {@code true} — ввод поглощён
	 */
	public final boolean mouseClicked(double mx, double my, int btn) {
		if (mx < overlayX || mx >= overlayX + overlayW || my < overlayY || my >= overlayY + overlayH) {
			close();
			return true;
		}

		return onMouseClicked(mx, my, btn);
	}

	/** Обрабатывает клик внутри окна оверлея. */
	protected abstract boolean onMouseClicked(double mx, double my, int btn);

	/** Обрабатывает скролл мыши. */
	public boolean mouseScrolled(double mx, double my, double vAmt) {
		return mx >= overlayX && mx < overlayX + overlayW
			&& my >= overlayY && my < overlayY + overlayH;
	}

	/**
	 * Обрабатывает нажатие клавиши.
	 * Escape — закрывает оверлей.
	 */
	public boolean keyPressed(KeyInput input) {
		if (input.isEscape()) {
			close();
			return true;
		}

		return onKeyPressed(input);
	}

	/** Обрабатывает нажатие клавиши (кроме Escape). */
	protected boolean onKeyPressed(KeyInput input) {
		return false;
	}

	/** Обрабатывает ввод символа. */
	public boolean charTyped(CharInput input) {
		return false;
	}

	/** Закрывает оверлей, вызывая колбэк. */
	protected final void close() {
		onClose.run();
	}

	protected static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}
}
