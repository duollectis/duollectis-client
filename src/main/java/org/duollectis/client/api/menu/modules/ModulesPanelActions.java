package org.duollectis.client.api.menu.modules;

import org.duollectis.client.api.menu.main.ClientScreen;
import org.duollectis.client.api.menu.main.ClientScreenLayout;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.SettingGroup;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;

/**
 * Утилита обработки кликов по панели модулей: переключение модулей,
 * выбор модуля для настроек и назначение клавиш-биндов.
 */
public final class ModulesPanelActions {

	private ModulesPanelActions() {}

	public static boolean handleModuleListClick(ClientScreen screen, double mx, double my, int btn) {
		List<Module> modules = screen.modules;
		int visibleCount = ClientScreenLayout.listAreaH() / ENTRY_H;
		int startIndex = Math.clamp(screen.listScroll, 0, Math.max(0, modules.size() - visibleCount));

		for (int i = startIndex; i < modules.size(); i++) {
			int entryY = screen.panelY + PAD + (i - startIndex) * ENTRY_H;

			if (entryY + ENTRY_H > screen.panelY + PAD + ClientScreenLayout.listAreaH()) {
				break;
			}

			if (my < entryY || my >= entryY + ENTRY_H) {
				continue;
			}

			if (btn == GLFW.GLFW_MOUSE_BUTTON_2) {
				screen.selectModule(modules.get(i));
			} else if (btn == GLFW.GLFW_MOUSE_BUTTON_1) {
				modules.get(i).toggle();
			}

			return true;
		}

		return false;
	}

	/**
	 * Обрабатывает ПКМ-клик по кнопке бинда в строке модуля.
	 * Если клик попал на кнопку — переводит экран в режим прослушивания клавиши для данного модуля.
	 *
	 * @return {@code true} если клик был поглощён кнопкой бинда
	 */
	public static boolean handleBindButtonClick(ClientScreen screen, double mx, double my) {
		List<Module> modules = screen.modules;
		int visibleCount = ClientScreenLayout.listAreaH() / ENTRY_H;
		int startIndex = Math.clamp(screen.listScroll, 0, Math.max(0, modules.size() - visibleCount));

		for (int i = startIndex; i < modules.size(); i++) {
			int entryY = screen.panelY + PAD + (i - startIndex) * ENTRY_H;

			if (entryY + ENTRY_H > screen.panelY + PAD + ClientScreenLayout.listAreaH()) {
				break;
			}

			if (my < entryY || my >= entryY + ENTRY_H) {
				continue;
			}

			int btnX = screen.panelX + LIST_W - BIND_BTN_PAD - BIND_BTN_W;
			int btnY = entryY + (ENTRY_H - BIND_BTN_H) / 2;

			if (mx < btnX || mx >= btnX + BIND_BTN_W || my < btnY || my >= btnY + BIND_BTN_H) {
				return false;
			}

			if (screen.activeTab != ClientScreen.Tab.MODULES) {
				screen.switchTab(ClientScreen.Tab.MODULES);
			}

			screen.bindListeningModule = modules.get(i);
			return true;
		}

		return false;
	}

	/**
	 * Обрабатывает клик по иконке сброса конкретной настройки.
	 * Итерирует все видимые настройки выбранного модуля, вычисляя их позиции
	 * аналогично рендереру, и при попадании клика сбрасывает только эту настройку.
	 *
	 * @return {@code true} если клик был поглощён иконкой сброса
	 */
	public static boolean handleSettingResetClick(ClientScreen screen, double mx, double my) {
		if (screen.selectedModule == null) {
			return false;
		}

		int iconX = ModulesPanelRenderer.settingResetIconX(screen.panelX);

		if (mx < iconX || mx >= iconX + SETTING_RESET_W) {
			return false;
		}

		int panelTop = screen.panelY + PAD + ROW_H;
		int panelBottom = screen.panelY + PANEL_H - PAD;
		int y = panelTop - screen.settingsScroll;
		int dividerH = SETTING_DIVIDER_PAD * 2 + 1;

		for (SettingGroup group : screen.selectedModule.getSettings().getGroups()) {
			y += ROW_H;

			List<Setting<?>> visibleSettings = group.settings()
				.stream()
				.filter(Setting::isVisible)
				.toList();

			for (int i = 0; i < visibleSettings.size(); i++) {
				Setting<?> setting = visibleSettings.get(i);
				int itemH = ModulesPanelRenderer.itemHeight(screen.textRenderer(), setting);

				int iconY = ModulesPanelRenderer.settingResetIconY(y, itemH);

				if (my >= iconY && my < iconY + SETTING_RESET_H) {
					setting.reset();
					screen.rebuildSettingsWidgets();
					return true;
				}

				y += itemH;

				if (i < visibleSettings.size() - 1) {
					y += dividerH;
				}
			}
		}

		return false;
	}

	public static boolean handleTabClick(ClientScreen screen, double mx, double my) {
		int tabsY = screen.panelY + PAD + ClientScreenLayout.listAreaH() + (TAB_ROW_H - TAB_BTN_H) / 2;
		int tabsX = screen.panelX + PAD;
		int modulesTabX = tabsX;
		int accountsTabX = tabsX + TAB_BTN_W + TAB_BTN_GAP;

		if (mx < screen.panelX || mx >= screen.panelX + LIST_W) {
			return false;
		}

		if (my < tabsY || my >= tabsY + TAB_BTN_H) {
			return false;
		}

		if (mx >= modulesTabX && mx < modulesTabX + TAB_BTN_W) {
			screen.switchTab(ClientScreen.Tab.MODULES);
			return true;
		}

		if (mx >= accountsTabX && mx < accountsTabX + TAB_BTN_W) {
			screen.switchTab(ClientScreen.Tab.ACCOUNTS);
			return true;
		}

		return false;
	}
}
