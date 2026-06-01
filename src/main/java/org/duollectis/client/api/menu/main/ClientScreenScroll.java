package org.duollectis.client.api.menu.main;

import org.duollectis.client.api.menu.modules.ModulesPanelRenderer;
import org.duollectis.client.api.settings.Setting;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;

final class ClientScreenScroll {

	private ClientScreenScroll() {}

	static void interpolate(ClientScreen screen, float delta) {
		float speed = 1f - (float) Math.pow(0.01, delta);
		screen.settingsScrollSmooth += (screen.settingsScrollTarget - screen.settingsScrollSmooth) * speed;

		int newScroll = Math.round(screen.settingsScrollSmooth);

		if (newScroll == screen.settingsScroll) {
			return;
		}

		screen.settingsScroll = newScroll;
		screen.rebuildSettingsWidgets();
	}

	static boolean isScrolling(ClientScreen screen) {
		return Math.abs(screen.settingsScrollTarget - screen.settingsScrollSmooth) > 0.5f;
	}

	static int computeMaxScroll(ClientScreen screen) {
		if (screen.selectedModule == null) {
			return 0;
		}

		int dividerH = SETTING_DIVIDER_PAD * 2 + 1;
		int visibleH = PANEL_H - PAD * 2 - ROW_H;
		int contentH = 0;

		for (var group : screen.selectedModule.getSettings().getGroups()) {
			contentH += ROW_H;

			var visibleSettings = group.settings()
				.stream()
				.filter(Setting::isVisible)
				.toList();

			for (int i = 0; i < visibleSettings.size(); i++) {
				contentH += ModulesPanelRenderer.itemHeight(screen.textRenderer(), visibleSettings.get(i));

				if (i < visibleSettings.size() - 1) {
					contentH += dividerH;
				}
			}
		}

		return Math.max(0, contentH - visibleH);
	}
}
