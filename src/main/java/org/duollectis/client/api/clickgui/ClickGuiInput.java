package org.duollectis.client.api.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.duollectis.client.api.clickgui.widget.ClickGuiSettingWidget;
import org.duollectis.client.api.clickgui.widget.SliderWidget;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.settings.Setting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.duollectis.client.api.clickgui.ClickGuiLayout.*;

/** Обработчик ввода ClickGui: клики, перетаскивание панелей и виджетов, скролл, биндинг, оверлеи. */
final class ClickGuiInput {

	private ClickGuiInput() {}

	static boolean mouseClicked(ClickGuiScreen screen, Click event) {
		// Активный оверлей перехватывает весь ввод с наивысшим приоритетом
		if (screen.activeOverlay != null) {
			return screen.activeOverlay.mouseClicked(event.x(), event.y(), event.button());
		}

		double mx = event.x();
		double my = event.y();
		int btn = event.button();

		// Режим прослушивания бинда: ЛКМ отменяет
		if (screen.bindListeningModule != null && btn == GLFW.GLFW_MOUSE_BUTTON_1) {
			screen.bindListeningModule = null;
			return true;
		}

		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			if (handlePanelClick(screen, panel, mx, my, btn)) {
				return true;
			}
		}

		return false;
	}

	private static boolean handlePanelClick(
			ClickGuiScreen screen,
			ClickGuiScreen.CategoryPanel panel,
			double mx,
			double my,
			int btn
	) {
		int w = panel.width;
		boolean onHeader = mx >= panel.x && mx < panel.x + w
			&& my >= panel.y && my < panel.y + HEADER_H;

		// ЛКМ по заголовку — начать перетаскивание
		if (onHeader && btn == GLFW.GLFW_MOUSE_BUTTON_1) {
			panel.dragging = true;
			panel.dragOffsetX = mx - panel.x;
			panel.dragOffsetY = my - panel.y;
			return true;
		}

		// ПКМ по заголовку — свернуть/развернуть
		if (onHeader && btn == GLFW.GLFW_MOUSE_BUTTON_2) {
			panel.collapsed = !panel.collapsed;

			if (panel.collapsed) {
				for (Module module : screen.modulesFor(panel.category)) {
					screen.expandedModules.remove(module);
					screen.settingsScroll.remove(module);
					screen.moduleWidgets.remove(module);
				}
			}

			return true;
		}

		if (panel.collapsed) {
			return onHeader;
		}

		List<Module> modules = screen.modulesFor(panel.category);
		int y = panel.y + HEADER_H;

		for (Module module : modules) {
			int moduleBottom = y + MODULE_H;

			if (my >= y && my < moduleBottom && mx >= panel.x && mx < panel.x + w) {
				return handleModuleClick(screen, panel, module, mx, my, btn, y, w);
			}

			y = moduleBottom;

			if (screen.expandedModules.contains(module)) {
				List<ClickGuiSettingWidget> widgets = screen.widgetsFor(module);
				int settingsH = ClickGuiRenderer.computeWidgetsHeight(widgets);

				if (my >= y && my < y + settingsH && mx >= panel.x && mx < panel.x + w) {
					return handleWidgetsClick(screen, module, widgets, panel.x, mx, my, btn, y);
				}

				y += settingsH;
			}
		}

		return false;
	}

	private static boolean handleModuleClick(
			ClickGuiScreen screen,
			ClickGuiScreen.CategoryPanel panel,
			Module module,
			double mx,
			double my,
			int btn,
			int moduleY,
			int w
	) {
		int btnX = panel.x + w - BIND_BTN_W - 2;
		int btnY = moduleY + (MODULE_H - BIND_BTN_H) / 2;

		boolean onBindBtn = mx >= btnX && mx < btnX + BIND_BTN_W
			&& my >= btnY && my < btnY + BIND_BTN_H;

		if (onBindBtn && btn == GLFW.GLFW_MOUSE_BUTTON_2) {
			screen.bindListeningModule = module;
			return true;
		}

		if (btn == GLFW.GLFW_MOUSE_BUTTON_2 && !onBindBtn) {
			boolean hasSettings = module.getSettings().all()
				.stream()
				.anyMatch(Setting::isVisible);

			if (hasSettings) {
				screen.toggleExpanded(module);
			}

			return true;
		}

		if (btn == GLFW.GLFW_MOUSE_BUTTON_1 && !onBindBtn) {
			module.toggle();
			return true;
		}

		return false;
	}

	private static boolean handleWidgetsClick(
			ClickGuiScreen screen,
			Module module,
			List<ClickGuiSettingWidget> widgets,
			int panelX,
			double mx,
			double my,
			int btn,
			int settingsStartY
	) {
		int scroll = screen.settingsScroll.getOrDefault(module, 0);
		int y = settingsStartY - scroll;

		for (ClickGuiSettingWidget widget : widgets) {
			int rowH = widget.height();

			if (my >= y && my < y + rowH) {
				widget.mouseClicked(panelX, y, findPanelWidth(screen, panelX), mx, my, btn);
				return true;
			}

			y += rowH;
		}

		return true;
	}

	static boolean mouseDragged(ClickGuiScreen screen, Click event) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_1) {
			return false;
		}

		// Оверлей перехватывает drag
		if (screen.activeOverlay != null) {
			return false;
		}

		// Перетаскивание виджетов имеет приоритет над перетаскиванием панелей
		for (Module module : screen.expandedModules) {
			List<ClickGuiSettingWidget> widgets = screen.widgetsFor(module);
			int panelX = findPanelXForModule(screen, module);

			if (panelX < 0) {
				continue;
			}

			int panelW = findPanelWidth(screen, panelX);

			for (ClickGuiSettingWidget widget : widgets) {
				if (widget.mouseDragged(panelX, 0, panelW, event.x())) {
					return true;
				}
			}
		}

		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			if (!panel.dragging) {
				continue;
			}

			panel.x = (int) (event.x() - panel.dragOffsetX);
			panel.y = (int) (event.y() - panel.dragOffsetY);
			return true;
		}

		return false;
	}

	static void mouseReleased(ClickGuiScreen screen, Click event) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_1) {
			return;
		}

		for (Module module : screen.expandedModules) {
			for (ClickGuiSettingWidget widget : screen.widgetsFor(module)) {
				widget.mouseReleased();
			}
		}

		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			panel.dragging = false;
		}
	}

	static boolean mouseScrolled(ClickGuiScreen screen, double mx, double my, double vAmt) {
		// Оверлей перехватывает скролл
		if (screen.activeOverlay != null) {
			return screen.activeOverlay.mouseScrolled(mx, my, vAmt);
		}

		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			if (panel.collapsed) {
				continue;
			}

			int w = panel.width;
			List<Module> modules = screen.modulesFor(panel.category);
			int y = panel.y + HEADER_H;

			for (Module module : modules) {
				y += MODULE_H;

				if (!screen.expandedModules.contains(module)) {
					continue;
				}

				List<ClickGuiSettingWidget> widgets = screen.widgetsFor(module);
				int settingsH = ClickGuiRenderer.computeWidgetsHeight(widgets);
				int totalContentH = widgets.stream().mapToInt(ClickGuiSettingWidget::height).sum();

				if (mx >= panel.x && mx < panel.x + w && my >= y && my < y + settingsH) {
					int current = screen.settingsScroll.getOrDefault(module, 0);
					int maxScroll = Math.max(0, totalContentH - settingsH);
					int updated = (int) Math.clamp(current - vAmt * SETTING_INLINE_H, 0, maxScroll);
					screen.settingsScroll.put(module, updated);
					return true;
				}

				y += settingsH;
			}
		}

		return false;
	}

	static boolean keyPressed(ClickGuiScreen screen, KeyInput input) {
		// Оверлей перехватывает клавиатуру
		if (screen.activeOverlay != null) {
			return screen.activeOverlay.keyPressed(input);
		}

		// Режим прослушивания бинда
		if (screen.bindListeningModule != null) {
			if (input.isEscape()) {
				screen.bindListeningModule.clearBind();
			}
			else {
				screen.bindListeningModule.setKeyCode(input.key());
			}

			screen.bindListeningModule = null;
			return true;
		}

		// Делегируем нажатие клавиши активным виджетам с текстовым вводом
		for (Module module : screen.expandedModules) {
			for (ClickGuiSettingWidget widget : screen.widgetsFor(module)) {
				if (widget instanceof SliderWidget sliderWidget && sliderWidget.isEditing()) {
					return sliderWidget.keyPressed(input);
				}
			}
		}

		return false;
	}

	static boolean charTyped(ClickGuiScreen screen, CharInput input) {
		// Оверлей перехватывает ввод символов
		if (screen.activeOverlay != null) {
			return screen.activeOverlay.charTyped(input);
		}

		for (Module module : screen.expandedModules) {
			for (ClickGuiSettingWidget widget : screen.widgetsFor(module)) {
				if (widget instanceof SliderWidget sliderWidget && sliderWidget.isEditing()) {
					return sliderWidget.charTyped(input);
				}
			}
		}

		return false;
	}

	private static int findPanelXForModule(ClickGuiScreen screen, Module module) {
		var info = module.getClass().getAnnotation(org.duollectis.client.api.modules.ModuleInfo.class);

		if (info == null) {
			return -1;
		}

		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			if (panel.category == info.category()) {
				return panel.x;
			}
		}

		return -1;
	}

	private static int findPanelWidth(ClickGuiScreen screen, int panelX) {
		for (ClickGuiScreen.CategoryPanel panel : screen.panels) {
			if (panel.x == panelX) {
				return panel.width;
			}
		}

		return PANEL_W;
	}
}
