package org.duollectis.client.api.menu.main;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.duollectis.client.api.i18n.ClientLocale;
import org.duollectis.client.api.menu.accounts.AccountsPanelActions;
import org.duollectis.client.api.menu.modules.ModulesPanelActions;
import org.duollectis.client.api.menu.widget.*;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;

final class ClientScreenInput {

	private ClientScreenInput() {}

	static boolean mouseClicked(ClientScreen screen, Click event, boolean handled) {
		if (screen.draggingPanel) {
			return true;
		}

		double mx = event.x();
		double my = event.y();
		int btn = event.button();

		if (screen.bindListeningModule != null && btn == 0) {
			screen.bindListeningModule = null;
			return true;
		}

		if (screen.activeTab == ClientScreen.Tab.ACCOUNTS && screen.confirmDialog != null && btn == 0) {
			boolean consumed = screen.confirmDialog.handleClick(mx, my, screen.width, screen.height);
			screen.confirmDialog = null;
			return consumed;
		}

		if (ModulesPanelActions.handleTabClick(screen, mx, my)) {
			return true;
		}

		if (screen.activeTab == ClientScreen.Tab.ACCOUNTS) {
			if (AccountsPanelActions.handleAccountsPanelClick(screen, mx, my, btn)) {
				return true;
			}
		}

		int listRight = screen.panelX + LIST_W;
		int panelRight = screen.panelX + PANEL_W;
		int contentTop = screen.panelY + PAD + ROW_H;
		int panelBottom = screen.panelY + PANEL_H - PAD;

		boolean inListPanel = mx >= screen.panelX
				&& mx < listRight
				&& my >= screen.panelY
				&& my < screen.panelY + PANEL_H;

		boolean inSettingsPanel = mx >= listRight
				&& mx < panelRight
				&& my >= screen.panelY
				&& my < screen.panelY + PANEL_H;

		if (inSettingsPanel && screen.isScrolling()) {
			return true;
		}

		if (inSettingsPanel
				&& screen.activeTab == ClientScreen.Tab.MODULES
				&& ModulesPanelActions.handleSettingResetClick(screen, mx, my)
		) {
			return true;
		}

		for (EnumDropdownWidget<ClientLocale> dropdown : screen.langDropdowns) {
			if (dropdown.handleClick(mx, my)) {
				return true;
			}
		}

		boolean inSettingsClip = mx >= listRight
				&& mx < panelRight
				&& my >= contentTop
				&& my < panelBottom;

		if (inSettingsClip) {
			boolean hitField = screen.textFields.stream().anyMatch(f -> f.isMouseOver(mx, my));

			if (hitField) {
				for (TextFieldWidget field : screen.textFields) {
					if (field.isMouseOver(mx, my)) {
						screen.setFocused(field);
						return true;
					}
				}
			}
			else {
				screen.setFocused(null);
			}

			for (ItemListWidget widget : screen.itemListWidgets) {
				if (widget.mouseClicked(event, handled)) {
					return true;
				}
			}

			for (EnumDropdownWidget<?> dropdown : screen.dropdowns) {
				if (dropdown.handleClick(mx, my)) {
					return true;
				}
			}

			for (StyledSliderWidget slider : screen.sliders) {
				if (slider.handleClick(mx, my)) {
					return true;
				}
			}

			for (BoolToggleWidget toggle : screen.boolToggles) {
				if (toggle.handleClick(mx, my)) {
					return true;
				}
			}

			for (StyledToggleButton toggle : screen.toggles) {
				if (toggle.handleClick(mx, my)) {
					return true;
				}
			}
		}
		else {
			screen.setFocused(null);
		}

		if (inListPanel && btn == 1 && ModulesPanelActions.handleBindButtonClick(screen, mx, my)) {
			return true;
		}

		if (inListPanel && ModulesPanelActions.handleModuleListClick(screen, mx, my, btn)) {
			return true;
		}

		if (btn == 0
				&& mx >= screen.panelX
				&& mx < screen.panelX + PANEL_W
				&& my >= screen.panelY
				&& my < screen.panelY + PANEL_H
		) {
			screen.draggingPanel = true;
			screen.dragOffsetX = mx - screen.panelX;
			screen.dragOffsetY = my - screen.panelY;
			return true;
		}

		return false;
	}

	static boolean mouseDragged(ClientScreen screen, Click event) {
		if (event.button() != 0) {
			return false;
		}

		if (screen.draggingPanel) {
			int maxW = screen.width - PANEL_W;
			int maxH = screen.height - PANEL_H;
			if (maxW < 0 || maxH < 0) {
				return false;
			}

			screen.panelX = (int) Math.clamp(event.x() - screen.dragOffsetX, 0, maxW);
			screen.panelY = (int) Math.clamp(event.y() - screen.dragOffsetY, 0, maxH);
			screen.rebuildSettingsWidgets();
			return true;
		}

		for (StyledSliderWidget slider : screen.sliders) {
			if (slider.handleDrag(event.x())) {
				return true;
			}
		}

		return false;
	}

	static boolean mouseReleased(ClientScreen screen, Click event) {
		if (event.button() != 0) {
			return false;
		}

		screen.draggingPanel = false;

		for (StyledSliderWidget slider : screen.sliders) {
			slider.handleRelease();
		}

		return false;
	}

	static boolean mouseScrolled(ClientScreen screen, double mx, double my, double vAmt) {
		if (screen.activeTab == ClientScreen.Tab.ACCOUNTS) {
			return AccountsPanelActions.handleAccountsScroll(screen, mx, my, vAmt);
		}

		int listRight = screen.panelX + LIST_W;
		int panelRight = screen.panelX + PANEL_W;

		if (mx >= screen.panelX && mx < listRight && my >= screen.panelY && my < screen.panelY + PANEL_H) {
			int visibleCount = listAreaH() / ENTRY_H;
			screen.listScroll = Math.clamp(
					(int) (screen.listScroll - vAmt),
					0,
					Math.max(0, screen.modules.size() - visibleCount)
			);
			return true;
		}

		if (mx >= listRight && mx < panelRight && my >= screen.panelY && my < screen.panelY + PANEL_H) {
			int step = screen.textRenderer().fontHeight + NAME_GAP + ROW_H;
			int maxScroll = ClientScreenScroll.computeMaxScroll(screen);
			screen.settingsScrollTarget = Math.clamp(
					screen.settingsScrollTarget + (float) (-vAmt * step),
					0,
					maxScroll
			);
			return true;
		}

		return false;
	}

	static boolean keyPressed(ClientScreen screen, KeyInput input) {
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

		if (screen.activeTab == ClientScreen.Tab.ACCOUNTS
				&& screen.accGroupRenameField != null
				&& screen.accGroupRenameField.isFocused()
		) {
			if (input.isEnter()) {
				AccountsPanelActions.commitGroupRename(screen);
				return true;
			}

			if (input.isEscape()) {
				AccountsPanelActions.cancelGroupRename(screen);
				return true;
			}

			screen.accGroupRenameField.keyPressed(input);
			return true;
		}

		if (screen.activeTab == ClientScreen.Tab.ACCOUNTS
				&& screen.accAccountRenameField != null
				&& screen.accAccountRenameField.isFocused()
		) {
			if (input.isEnter()) {
				AccountsPanelActions.commitAccountRename(screen);
				return true;
			}

			if (input.isEscape()) {
				AccountsPanelActions.cancelAccountRename(screen);
				return true;
			}

			screen.accAccountRenameField.keyPressed(input);
			return true;
		}

		for (TextFieldWidget field : screen.textFields) {
			if (field.isFocused() && field.keyPressed(input)) {
				return true;
			}
		}

		for (ItemListWidget widget : screen.itemListWidgets) {
			if (widget.isFocused() && widget.keyPressed(input)) {
				return true;
			}
		}

		return false;
	}

	static boolean charTyped(ClientScreen screen, CharInput input) {
		if (screen.activeTab == ClientScreen.Tab.ACCOUNTS
				&& screen.accGroupRenameField != null
				&& screen.accGroupRenameField.isFocused()
		) {
			screen.accGroupRenameField.charTyped(input);
			return true;
		}

		if (screen.activeTab == ClientScreen.Tab.ACCOUNTS
				&& screen.accAccountRenameField != null
				&& screen.accAccountRenameField.isFocused()
		) {
			screen.accAccountRenameField.charTyped(input);
			return true;
		}

		for (TextFieldWidget field : screen.textFields) {
			if (field.isFocused() && field.charTyped(input)) {
				return true;
			}
		}

		for (ItemListWidget widget : screen.itemListWidgets) {
			if (widget.isFocused() && widget.charTyped(input)) {
				return true;
			}
		}

		return false;
	}
}
