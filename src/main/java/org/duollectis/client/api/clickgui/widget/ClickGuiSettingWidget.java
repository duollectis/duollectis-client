package org.duollectis.client.api.clickgui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Контракт виджета настройки в ClickGui.
 * Каждый тип настройки реализует свой виджет, который умеет
 * рендерить себя и обрабатывать ввод мыши в заданных координатах.
 */
public interface ClickGuiSettingWidget {

	/** Высота строки виджета в пикселях. */
	int height();

	/**
	 * Рендерит виджет в заданных координатах.
	 *
	 * @param ctx          контекст рисования
	 * @param textRenderer рендерер шрифта
	 * @param x            левый край панели
	 * @param y            верхний край строки
	 * @param width        ширина панели
	 * @param mx           позиция мыши X
	 * @param my           позиция мыши Y
	 */
	void render(DrawContext ctx, TextRenderer textRenderer, int x, int y, int width, int mx, int my);

	/**
	 * Обрабатывает клик мыши.
	 *
	 * @param x     левый край панели
	 * @param y     верхний край строки
	 * @param width ширина панели
	 * @param mx    позиция мыши X
	 * @param my    позиция мыши Y
	 * @param btn   кнопка мыши (GLFW_MOUSE_BUTTON_*)
	 * @return {@code true} если клик был обработан
	 */
	boolean mouseClicked(int x, int y, int width, double mx, double my, int btn);

	/**
	 * Обрабатывает перетаскивание мыши (только ЛКМ).
	 * По умолчанию — не обрабатывает.
	 */
	default boolean mouseDragged(int x, int y, int width, double mx) {
		return false;
	}

	/** Вызывается при отпускании кнопки мыши. */
	default void mouseReleased() {}
}
