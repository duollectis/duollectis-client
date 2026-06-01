package org.duollectis.client.api.clickgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.clickgui.widget.ClickGuiSettingWidget;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.settings.Setting;

import java.util.List;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/**
 * Рендерер ClickGui: отрисовывает колонки категорий, строки модулей,
 * развёрнутые панели настроек и кнопки биндов.
 * Рендеринг каждой настройки делегируется соответствующему {@link ClickGuiSettingWidget}.
 */
public final class ClickGuiRenderer {

	private ClickGuiRenderer() {}

	/**
	 * Рендерит все панели категорий.
	 * При первом открытии (флаг {@code panelPositionsDirty}) пересчитывает X-позиции
	 * панелей на основе их реальных ширин, чтобы они не наезжали друг на друга.
	 */
	public static void renderAll(
			ClickGuiScreen screen,
			DrawContext ctx,
			int mx,
			int my,
			TextRenderer textRenderer
	) {
		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			panel.width = computePanelWidth(screen, panel, textRenderer);
		}

		if (screen.panelPositionsDirty) {
			repositionPanels(screen);
			screen.panelPositionsDirty = false;
		}

		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			renderPanel(screen, ctx, mx, my, textRenderer, panel);
		}
	}

	/** Расставляет панели слева направо с учётом их реальных ширин и зазора. */
	private static void repositionPanels(ClickGuiScreen screen) {
		int x = 10;
		int startY = 10;

		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			panel.x = x;
			panel.y = startY;
			x += panel.width + PANEL_GAP;
		}
	}

	/**
	 * Вычисляет оптимальную ширину панели по самому длинному имени модуля.
	 * Учитывает отступы для точки, стрелки разворота и кнопки бинда.
	 */
	private static int computePanelWidth(
			ClickGuiScreen screen,
			ClickGuiScreen.CategoryPanel panel,
			TextRenderer textRenderer
	) {
		List<Module> modules = screen.modulesFor(panel.category);
		int maxNameW = 0;

		for (Module module : modules) {
			maxNameW = Math.max(maxNameW, textRenderer.getWidth(module.getName()));
		}

		int needed = maxNameW + PANEL_W_PADDING;
		return Math.clamp(needed, PANEL_MIN_W, PANEL_MAX_W);
	}

	private static void renderPanel(
			ClickGuiScreen screen,
			DrawContext ctx,
			int mx,
			int my,
			TextRenderer textRenderer,
			ClickGuiScreen.CategoryPanel panel
	) {
		int w = panel.width;
		List<Module> modules = screen.modulesFor(panel.category);
		int totalH = computePanelHeight(screen, modules, panel.collapsed);

		ctx.fill(panel.x + 2, panel.y + 2, panel.x + w + 2, panel.y + totalH + 2, 0x55000000);
		ctx.fill(panel.x, panel.y, panel.x + w, panel.y + totalH, COLOR_PANEL_BG);
		drawBorder(ctx, panel.x, panel.y, w, totalH, COLOR_PANEL_BORDER);

		renderHeader(ctx, mx, my, textRenderer, panel, w);

		if (panel.collapsed) {
			return;
		}

		int y = panel.y + HEADER_H;

		for (Module module : modules) {
			y = renderModule(screen, ctx, mx, my, textRenderer, panel, module, y, w);
		}
	}

	private static void renderHeader(
			DrawContext ctx,
			int mx,
			int my,
			TextRenderer textRenderer,
			ClickGuiScreen.CategoryPanel panel,
			int w
	) {
		boolean hovered = mx >= panel.x && mx < panel.x + w
			&& my >= panel.y && my < panel.y + HEADER_H;

		int headerBg = hovered ? COLOR_HEADER_ACTIVE : COLOR_HEADER_BG;
		ctx.fill(panel.x, panel.y, panel.x + w, panel.y + HEADER_H, headerBg);
		drawBorder(ctx, panel.x, panel.y, w, HEADER_H, COLOR_PANEL_BORDER);

		String arrow = panel.collapsed ? "▶" : "▼";
		String label = categoryName(panel.category);
		int arrowW = textRenderer.getWidth(arrow);
		int labelX = panel.x + PAD + arrowW + 3;
		int textY = panel.y + (HEADER_H - 8) / 2;

		ctx.drawTextWithShadow(textRenderer, Text.literal(arrow), panel.x + PAD, textY, COLOR_EXPANDED_ARROW);
		ctx.drawTextWithShadow(textRenderer, Text.literal(label), labelX, textY, COLOR_TEXT_HEADER);
	}

	/**
	 * Рендерит строку модуля и, если он развёрнут, его настройки через виджеты.
	 *
	 * @return Y-координата следующего элемента
	 */
	private static int renderModule(
			ClickGuiScreen screen,
			DrawContext ctx,
			int mx,
			int my,
			TextRenderer textRenderer,
			ClickGuiScreen.CategoryPanel panel,
			Module module,
			int y,
			int w
	) {
		boolean active = module.isActive();
		boolean expanded = screen.expandedModules.contains(module);
		boolean hovered = mx >= panel.x && mx < panel.x + w
			&& my >= y && my < y + MODULE_H;

		int bg = active
			? (hovered ? COLOR_MODULE_ACTIVE_HOVER : COLOR_MODULE_ACTIVE)
			: (hovered ? COLOR_MODULE_HOVER : COLOR_MODULE_BG);

		ctx.fill(panel.x, y, panel.x + w, y + MODULE_H, bg);

		int dotColor = active ? COLOR_ACTIVE_DOT : COLOR_INACTIVE_DOT;
		ctx.fill(panel.x + 3, y + MODULE_H / 2 - 2, panel.x + 6, y + MODULE_H / 2 + 2, dotColor);

		int nameColor = active ? COLOR_TEXT_ACTIVE : COLOR_TEXT_PRIMARY;
		int maxNameW = w - 12 - BIND_BTN_W - 6 - textRenderer.getWidth("▼") - 4;
		String name = textRenderer.trimToWidth(module.getName(), maxNameW);
		ctx.drawTextWithShadow(textRenderer, Text.literal(name), panel.x + 10, y + (MODULE_H - 8) / 2, nameColor);

		if (hasSettings(module)) {
			String expandArrow = expanded ? "▲" : "▼";
			int arrowColor = expanded ? COLOR_EXPANDED_ARROW : COLOR_TEXT_DIM;
			int arrowX = panel.x + w - BIND_BTN_W - 8 - textRenderer.getWidth(expandArrow);
			ctx.drawTextWithShadow(textRenderer, Text.literal(expandArrow), arrowX, y + (MODULE_H - 8) / 2, arrowColor);
		}

		renderBindButton(ctx, textRenderer, screen, module, panel.x, y, mx, my, w);

		y += MODULE_H;

		if (expanded) {
			y = renderSettings(screen, ctx, mx, my, textRenderer, panel, module, y, w);
		}

		return y;
	}

	private static void renderBindButton(
			DrawContext ctx,
			TextRenderer textRenderer,
			ClickGuiScreen screen,
			Module module,
			int panelX,
			int moduleY,
			int mx,
			int my,
			int w
	) {
		int btnX = panelX + w - BIND_BTN_W - 2;
		int btnY = moduleY + (MODULE_H - BIND_BTN_H) / 2;

		boolean listening = module == screen.bindListeningModule;
		boolean hovered = mx >= btnX && mx < btnX + BIND_BTN_W
			&& my >= btnY && my < btnY + BIND_BTN_H;

		int btnBg = listening ? COLOR_BTN_LISTEN
			: hovered ? COLOR_BTN_HOVER
			: COLOR_BTN_BG;

		ctx.fill(btnX, btnY, btnX + BIND_BTN_W, btnY + BIND_BTN_H, btnBg);
		drawBorder(ctx, btnX, btnY, BIND_BTN_W, BIND_BTN_H, COLOR_BTN_BORDER);

		String label = listening ? "..."
			: (module.getKeyName() == null ? "---" : module.getKeyName());

		String trimmed = textRenderer.getWidth(label) <= BIND_BTN_W - 4
			? label
			: trimToWidth(textRenderer, label, BIND_BTN_W - 4);

		int labelColor = listening ? COLOR_TEXT_ACTIVE : COLOR_TEXT_DIM;
		int labelX = btnX + (BIND_BTN_W - textRenderer.getWidth(trimmed)) / 2;
		int labelY = btnY + (BIND_BTN_H - 8) / 2;

		ctx.drawTextWithShadow(textRenderer, Text.literal(trimmed), labelX, labelY, labelColor);
	}

	/**
	 * Рендерит развёрнутую панель настроек модуля, делегируя каждую строку виджету.
	 *
	 * @return Y-координата следующего элемента после панели настроек
	 */
	private static int renderSettings(
			ClickGuiScreen screen,
			DrawContext ctx,
			int mx,
			int my,
			TextRenderer textRenderer,
			ClickGuiScreen.CategoryPanel panel,
			Module module,
			int startY,
			int w
	) {
		int scroll = screen.settingsScroll.getOrDefault(module, 0);
		List<ClickGuiSettingWidget> widgets = screen.widgetsFor(module);
		int totalSettingsH = computeWidgetsHeight(widgets);

		ctx.fill(panel.x, startY, panel.x + w, startY + totalSettingsH, COLOR_SETTINGS_BG);
		drawBorder(ctx, panel.x, startY, w, totalSettingsH, COLOR_BORDER);

		ctx.enableScissor(panel.x, startY, panel.x + w, startY + totalSettingsH);

		int y = startY - scroll;

		for (ClickGuiSettingWidget widget : widgets) {
			widget.render(ctx, textRenderer, panel.x, y, w, mx, my);
			y += widget.height();
		}

		ctx.disableScissor();

		return startY + totalSettingsH;
	}

	/** Вычисляет полную высоту панели категории с учётом развёрнутых настроек. */
	static int computePanelHeight(ClickGuiScreen screen, List<Module> modules, boolean collapsed) {
		if (collapsed) {
			return HEADER_H;
		}

		int h = HEADER_H;

		for (Module module : modules) {
			h += MODULE_H;

			if (screen.expandedModules.contains(module)) {
				h += computeWidgetsHeight(screen.widgetsFor(module));
			}
		}

		return h;
	}

	/** Суммирует высоты всех виджетов. */
	static int computeWidgetsHeight(List<ClickGuiSettingWidget> widgets) {
		int h = 0;

		for (ClickGuiSettingWidget widget : widgets) {
			h += widget.height();
		}

		return Math.max(h, SETTING_INLINE_H);
	}

	private static boolean hasSettings(Module module) {
		return module.getSettings().all()
			.stream()
			.anyMatch(Setting::isVisible);
	}

	private static String categoryName(Category category) {
		return switch (category) {
			case MOVEMENT -> "Movement";
			case RENDER -> "Render";
			case MISC -> "Misc";
			case NONE -> "Other";
		};
	}

	static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
		ctx.fill(x, y, x + w, y + 1, color);
		ctx.fill(x, y + h - 1, x + w, y + h, color);
		ctx.fill(x, y, x + 1, y + h, color);
		ctx.fill(x + w - 1, y, x + w, y + h, color);
	}

	private static String trimToWidth(TextRenderer textRenderer, String text, int maxWidth) {
		StringBuilder result = new StringBuilder();

		for (char c : text.toCharArray()) {
			if (textRenderer.getWidth(result + String.valueOf(c)) > maxWidth) {
				break;
			}

			result.append(c);
		}

		return result.toString();
	}
}
