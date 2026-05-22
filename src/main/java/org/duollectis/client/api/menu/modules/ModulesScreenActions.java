package org.duollectis.client.api.menu.modules;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.account.AccountSwitcher;
import org.duollectis.client.api.menu.widget.ConfirmDialog;
import org.duollectis.client.api.modules.Module;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

/**
 * Утилита обработки пользовательских действий на экране модулей:
 * клики по списку модулей, вкладкам и панели аккаунтов.
 */
final class ModulesScreenActions {

	@FunctionalInterface
	interface AccStatusSink {
		void set(String message, int color);
	}

	private ModulesScreenActions() {}

	static boolean handleModuleListClick(ModulesScreen screen, double mx, double my, int btn) {
		List<Module> modules = screen.modules;
		int visibleCount = (PANEL_H - PAD * 2) / ENTRY_H;
		int startIndex = Math.clamp(screen.listScroll, 0, Math.max(0, modules.size() - visibleCount));

		for (int i = startIndex; i < modules.size(); i++) {
			int entryY = screen.panelY + PAD + (i - startIndex) * ENTRY_H;

			if (entryY + ENTRY_H > screen.panelY + PANEL_H - PAD) {
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
	static boolean handleBindButtonClick(ModulesScreen screen, double mx, double my) {
		List<Module> modules = screen.modules;
		int visibleCount = (PANEL_H - PAD * 2) / ENTRY_H;
		int startIndex = Math.clamp(screen.listScroll, 0, Math.max(0, modules.size() - visibleCount));

		for (int i = startIndex; i < modules.size(); i++) {
			int entryY = screen.panelY + PAD + (i - startIndex) * ENTRY_H;

			if (entryY + ENTRY_H > screen.panelY + PANEL_H - PAD) {
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

			screen.bindListeningModule = modules.get(i);
			return true;
		}

		return false;
	}

	static boolean handleTabClick(ModulesScreen screen, double mx, double my) {
		int tabsY = screen.panelY + PAD + listAreaH() + (TAB_ROW_H - TAB_BTN_H) / 2;
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
			screen.switchTab(ModulesScreen.Tab.MODULES);
			return true;
		}

		if (mx >= accountsTabX && mx < accountsTabX + TAB_BTN_W) {
			screen.switchTab(ModulesScreen.Tab.ACCOUNTS);
			return true;
		}

		return false;
	}

	static boolean handleAccountsPanelClick(ModulesScreen screen, double mx, double my, int btn) {
		if (btn != 0 && btn != 1) {
			return false;
		}

		if (btn == 1) {
			return handleAccountRightClick(screen, mx, my);
		}

		if (screen.accRenamingGroup != null) {
			cancelGroupRename(screen);
		}

		if (screen.accRenamingAccount != null) {
			cancelAccountRename(screen);
		}

		if (hitsBtn(screen.accAddAccountBtn, mx, my)) {
			startAddAccount(screen);
			return true;
		}

		if (hitsBtn(screen.accAddGroupBtn, mx, my)) {
			addGroup(screen);
			return true;
		}

		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int contentW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int listH = accListH(screen);
		int listContentH = listH - ACC_ENTRY_H;

		if (mx >= contentX && mx < contentX + contentW && my >= listContentY && my < listContentY + listContentH) {
			int rowOffset = (int) (my - listContentY) / ACC_ENTRY_H + screen.accListScroll;

			if (handleGroupHeaderClick(screen, rowOffset, contentX, contentW, listContentY, mx, my)) {
				return true;
			}

			if (handleAccountRowButtonClick(screen, rowOffset, contentX, contentW, listContentY, mx, my)) {
				return true;
			}

			Account clicked = resolveClickedAccount(screen, rowOffset);
	
			if (clicked == null) {
				screen.accSelected = null;
				screen.setFocused(null);
				return true;
			}
	
			screen.accSelected = clicked;
			screen.setFocused(null);
			return true;
		}

		screen.setFocused(null);
		return false;
	}

	/**
	 * Обрабатывает клик по заголовку группы в режиме GROUP.
	 * Приоритет кнопок (справа налево): "→ сюда", "✕", "✎".
	 * Клик по левой части заголовка (стрелка) — сворачивает/разворачивает группу.
	 */
	private static boolean handleGroupHeaderClick(
			ModulesScreen screen,
			int rowOffset,
			int contentX,
			int contentW,
			int listContentY,
			double mx,
			double my
	) {
		if (screen.accSortMode != ModulesScreenLayout.AccSortMode.GROUP) {
			return false;
		}

		List<Object> rows = buildGroupedRows(screen.accCached, screen.collapsedGroups);

		if (rowOffset < 0 || rowOffset >= rows.size()) {
			return false;
		}

		Object row = rows.get(rowOffset);

		if (row instanceof String groupName) {
			if (groupName.isEmpty()) {
				return true;
			}

			int headerY = listContentY + (rowOffset - screen.accListScroll) * ACC_ENTRY_H;
			boolean headerHovered = my >= headerY && my < headerY + ACC_ENTRY_H;

			if (headerHovered == false) {
				return false;
			}

			boolean hasMoveBtn = screen.accSelected != null
					&& screen.accSelected.getGroup().equals(groupName) == false;

			// Кнопка "→ сюда" — только если аккаунт выбран и он не в этой группе
			if (hasMoveBtn && isMoveButtonHit(contentX, contentW, mx)) {
				moveAccountToGroup(screen, screen.accSelected, groupName);
				return true;
			}

			// Кнопка "✕" удалить группу
			if (isDeleteButtonHit(contentX, contentW, hasMoveBtn, mx)) {
				screen.confirmDialog = new ConfirmDialog(
						"Удалить группу «" + groupName + "»?",
						() -> deleteGroup(screen, groupName)
				);
				return true;
			}

			// Кнопка "✎" переименовать группу
			if (isRenameButtonHit(contentX, contentW, hasMoveBtn, mx)) {
				startGroupRename(screen, groupName, contentX, contentW, headerY);
				return true;
			}
	
			// Кнопка "↓" сдвинуть группу вниз
			if (isGroupDownArrowHit(contentX, contentW, hasMoveBtn, mx)) {
				AccountManager.getInstance().moveGroupDown(groupName);
				return true;
			}
	
			// Кнопка "↑" сдвинуть группу вверх
			if (isGroupUpArrowHit(contentX, contentW, hasMoveBtn, mx)) {
				AccountManager.getInstance().moveGroupUp(groupName);
				return true;
			}
	
			// Кнопка "⇅" отсортировать аккаунты группы по имени
			if (isGroupSortButtonHit(contentX, contentW, hasMoveBtn, mx)) {
				AccountManager.getInstance().sortGroupAccounts(groupName);
				screen.accStatus.set("Группа «" + groupName + "» отсортирована", 0xFF88CCFF);
				return true;
			}
	
			// Клик по остальной части заголовка — переключаем сворачивание
			boolean collapsed = screen.collapsedGroups.getOrDefault(groupName, false);
			screen.collapsedGroups.put(groupName, !collapsed);
			screen.accListScroll = 0;
			return true;
		}

		return false;
	}

	private static boolean isMoveButtonHit(int contentX, int contentW, double mx) {
		int btnW = 46;
		int btnX = contentX + contentW - btnW - 2;
		return mx >= btnX && mx < btnX + btnW;
	}

	private static boolean isDeleteButtonHit(int contentX, int contentW, boolean hasMoveBtn, double mx) {
		int moveBtnOffset = hasMoveBtn ? 48 : 0;
		int deleteBtnW = 18;
		int deleteBtnX = contentX + contentW - moveBtnOffset - deleteBtnW - 2;
		return mx >= deleteBtnX && mx < deleteBtnX + deleteBtnW;
	}

	private static boolean isRenameButtonHit(int contentX, int contentW, boolean hasMoveBtn, double mx) {
		int moveBtnOffset = hasMoveBtn ? 48 : 0;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int renameBtnX = contentX + contentW - moveBtnOffset - deleteBtnW - renameBtnW - 4;
		return mx >= renameBtnX && mx < renameBtnX + renameBtnW;
	}

	private static void moveAccountToGroup(ModulesScreen screen, Account account, String groupName) {
		AccountManager manager = AccountManager.getInstance();
		String oldGroup = account.getGroup();

		// Сохраняем старую группу как именованную, чтобы она не исчезла после перемещения последнего аккаунта
		if (oldGroup.isEmpty() == false) {
			manager.addNamedGroup(oldGroup);
		}

		account.setGroup(groupName);
		manager.save();
		screen.accStatus.set("Перемещён в: " + groupName, 0xFF88CCFF);
	}

	static void deleteGroup(ModulesScreen screen, String groupName) {
		AccountManager manager = AccountManager.getInstance();
		manager.removeNamedGroup(groupName);

		for (Account account : manager.getAccounts()) {
			if (account.getGroup().equals(groupName)) {
				account.setGroup("");
			}
		}

		manager.save();
		screen.accStatus.set("Группа удалена: " + groupName, 0xFFFF8888);
	}

	/**
	 * Запускает режим инлайн-переименования группы: создаёт TextFieldWidget поверх заголовка.
	 */
	static void startGroupRename(ModulesScreen screen, String groupName, int contentX, int contentW, int headerY) {
		screen.accRenamingGroup = groupName;

		TextFieldWidget field = new TextFieldWidget(
				screen.textRenderer(), contentX + 5, headerY + 1, contentW - 10, ACC_ENTRY_H - 2, Text.empty()
		);
		field.setText(groupName);
		field.setMaxLength(32);
		field.setDrawsBackground(false);

		screen.accGroupRenameField = field;
		screen.setFocused(field);
	}

	/** Применяет переименование: обновляет группу у всех аккаунтов с этим именем и сохраняет. */
	static void commitGroupRename(ModulesScreen screen) {
		if (screen.accRenamingGroup == null || screen.accGroupRenameField == null) {
			return;
		}

		String oldName = screen.accRenamingGroup;
		String newName = screen.accGroupRenameField.getText().trim();

		if (newName.isEmpty() == false && newName.equals(oldName) == false) {
			AccountManager manager = AccountManager.getInstance();
			List<Account> all = manager.getAccounts();

			for (Account account : all) {
				if (account.getGroup().equals(oldName)) {
					account.setGroup(newName);
				}
			}

			manager.renameNamedGroup(oldName, newName);
			manager.save();
			screen.accStatus.set("Группа переименована: " + newName, 0xFF88CCFF);
		}

		screen.accRenamingGroup = null;
		screen.accGroupRenameField = null;
		screen.setFocused(null);
	}

	/** Отменяет переименование без сохранения изменений. */
	static void cancelGroupRename(ModulesScreen screen) {
		screen.accRenamingGroup = null;
		screen.accGroupRenameField = null;
		screen.setFocused(null);
	}

	/** Добавляет новую пустую группу с уникальным именем "Новая группа N" и сохраняет её. */
	static void addGroup(ModulesScreen screen) {
		AccountManager manager = AccountManager.getInstance();
		TreeSet<String> existing = new TreeSet<>();

		for (Account a : manager.getAccounts()) {
			existing.add(a.getGroup());
		}

		existing.addAll(manager.getNamedGroups());

		int idx = 1;

		while (existing.contains("Новая группа " + idx)) {
			idx++;
		}

		String name = "Новая группа " + idx;
		manager.addNamedGroup(name);
		screen.accStatus.set("Группа создана: " + name, 0xFF88CCFF);
	}

	private static Account resolveClickedAccount(ModulesScreen screen, int rowOffset) {
		if (screen.accSortMode != ModulesScreenLayout.AccSortMode.GROUP) {
			if (rowOffset >= 0 && rowOffset < screen.accCached.size()) {
				return screen.accCached.get(rowOffset);
			}

			return null;
		}

		List<Object> rows = buildGroupedRows(screen.accCached, screen.collapsedGroups);

		if (rowOffset < 0 || rowOffset >= rows.size()) {
			return null;
		}

		Object row = rows.get(rowOffset);
		return row instanceof Account account ? account : null;
	}

	private static boolean hitsBtn(int[] btn, double mx, double my) {
		return mx >= btn[0] && mx < btn[0] + btn[2] && my >= btn[1] && my < btn[1] + btn[3];
	}

	static boolean handleAccountsScroll(ModulesScreen screen, double mx, double my, double vAmt) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int contentY = screen.panelY + PAD;
		int contentW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int listH = accListH(screen);

		if (mx >= contentX && mx < contentX + contentW && my >= contentY && my < contentY + listH) {
			int maxVis = Math.max(1, listH / ACC_ENTRY_H);
			int delta = -(int) Math.signum(vAmt);
			screen.accListScroll = Math.clamp(
					screen.accListScroll + delta,
					0,
					Math.max(0, screen.accCached.size() - maxVis)
			);
			return true;
		}

		return false;
	}

	/** Запускает inline-добавление нового аккаунта: открывает поле ввода поверх фиктивной строки. */
	static void startAddAccount(ModulesScreen screen) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int contentW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int listH = accListH(screen);
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int listContentH = listH - ACC_ENTRY_H;

		// Поле появляется в самом низу видимой области списка
		int fieldY = listContentY + listContentH - ACC_ENTRY_H;

		TextFieldWidget field = new TextFieldWidget(
				screen.textRenderer(), contentX + 5, fieldY + 1, contentW - 10, ACC_ENTRY_H - 2, Text.empty()
		);
		field.setPlaceholder(Text.literal("Никнейм (макс. 16)"));
		field.setMaxLength(16);
		field.setDrawsBackground(false);

		screen.accRenamingAccount = new Account("__new__");
		screen.accAccountRenameField = field;
		screen.setFocused(field);
	}

	/** Применяет inline-добавление: создаёт аккаунт с введённым именем. */
	static void commitAccountRename(ModulesScreen screen) {
		if (screen.accRenamingAccount == null || screen.accAccountRenameField == null) {
			return;
		}

		String newName = screen.accAccountRenameField.getText().trim();
		AccountManager manager = AccountManager.getInstance();

		// Режим добавления нового аккаунта
		if (screen.accRenamingAccount.getUsername().equals("__new__")) {
			if (newName.isEmpty()) {
				cancelAccountRename(screen);
				return;
			}

			if (manager.contains(newName)) {
				screen.accStatus.set("Аккаунт уже существует", 0xFFFFAA44);
				cancelAccountRename(screen);
				return;
			}

			manager.addAccount(new Account(newName));
			screen.accStatus.set("Добавлен: " + newName, 0xFF88FFAA);
		} else {
			// Режим переименования существующего аккаунта
			String oldName = screen.accRenamingAccount.getUsername();

			if (newName.isEmpty() == false && newName.equals(oldName) == false) {
				if (manager.contains(newName)) {
					screen.accStatus.set("Аккаунт уже существует", 0xFFFFAA44);
					cancelAccountRename(screen);
					return;
				}

				screen.accRenamingAccount.setUsername(newName);
				manager.save();
				screen.accStatus.set("Переименован: " + newName, 0xFF88CCFF);
			}
		}

		screen.accRenamingAccount = null;
		screen.accAccountRenameField = null;
		screen.setFocused(null);
	}

	/** Отменяет inline-редактирование аккаунта без сохранения. */
	static void cancelAccountRename(ModulesScreen screen) {
		screen.accRenamingAccount = null;
		screen.accAccountRenameField = null;
		screen.setFocused(null);
	}

	/**
	 * Обрабатывает клик по кнопкам "✕" и "✎" на строке аккаунта.
	 * Кнопки видны только при hover, поэтому проверяем позицию мыши.
	 */
	private static boolean handleAccountRowButtonClick(
			ModulesScreen screen,
			int rowOffset,
			int contentX,
			int contentW,
			int listContentY,
			double mx,
			double my
	) {
		Account account = resolveClickedAccount(screen, rowOffset);

		if (account == null) {
			return false;
		}

		int accountY = listContentY + (rowOffset - screen.accListScroll) * ACC_ENTRY_H;
		boolean rowHovered = my >= accountY && my < accountY + ACC_ENTRY_H;

		if (rowHovered == false) {
			return false;
		}

		int btnH = ACC_ENTRY_H - 2;
		int btnY = accountY + 1;

		if (isAccountApplyButtonHit(contentX, contentW, mx)) {
			switchAccount(screen, account);
			return true;
		}

		if (isAccountDeleteButtonHit(contentX, contentW, mx)) {
			Account toDelete = account;
			screen.confirmDialog = new ConfirmDialog(
					"Удалить аккаунт «" + toDelete.getUsername() + "»?",
					() -> {
						AccountManager.getInstance().removeAccount(toDelete);

						if (toDelete == screen.accSelected) {
							screen.accSelected = null;
						}

						screen.accStatus.set("Удалён: " + toDelete.getUsername(), 0xFFFF8888);
					}
			);
			return true;
		}

		if (isAccountRenameButtonHit(contentX, contentW, mx)) {
			startAccountRename(screen, account, contentX, contentW, accountY);
			return true;
		}

		if (account.getGroup().isEmpty() == false && isAccountUngroupButtonHit(contentX, contentW, mx)) {
			account.setGroup("");
			AccountManager.getInstance().save();
			screen.accStatus.set("Убран из группы: " + account.getUsername(), 0xFF6699FF);
			return true;
		}

		if (isAccountDownArrowHit(contentX, contentW, account.getGroup().isEmpty() == false, mx)) {
			AccountManager.getInstance().moveAccountDown(account);
			return true;
		}

		if (isAccountUpArrowHit(contentX, contentW, account.getGroup().isEmpty() == false, mx)) {
			AccountManager.getInstance().moveAccountUp(account);
			return true;
		}

		if (isAccountDupButtonHit(contentX, contentW, account.getGroup().isEmpty() == false, mx)) {
			AccountManager.getInstance().duplicateAccount(account);
			screen.accStatus.set("Дублирован: " + account.getUsername(), 0xFFCCAAFF);
			return true;
		}

		return false;
	}

	/** Запускает inline-переименование существующего аккаунта. */
	static void startAccountRename(ModulesScreen screen, Account account, int contentX, int contentW, int rowY) {
		screen.accRenamingAccount = account;

		TextFieldWidget field = new TextFieldWidget(
				screen.textRenderer(), contentX + 5, rowY + 1, contentW - 10, ACC_ENTRY_H - 2, Text.empty()
		);
		field.setText(account.getUsername());
		field.setMaxLength(16);
		field.setDrawsBackground(false);

		screen.accAccountRenameField = field;
		screen.setFocused(field);
	}

	private static boolean isAccountApplyButtonHit(int contentX, int contentW, double mx) {
		int applyBtnW = 18;
		int applyBtnX = contentX + contentW - applyBtnW - 2;
		return mx >= applyBtnX && mx < applyBtnX + applyBtnW;
	}

	private static boolean isAccountDeleteButtonHit(int contentX, int contentW, double mx) {
		int applyBtnW = 18;
		int deleteBtnW = 18;
		int deleteBtnX = contentX + contentW - applyBtnW - deleteBtnW - 4;
		return mx >= deleteBtnX && mx < deleteBtnX + deleteBtnW;
	}

	private static boolean isAccountRenameButtonHit(int contentX, int contentW, double mx) {
		int applyBtnW = 18;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int renameBtnX = contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - 6;
		return mx >= renameBtnX && mx < renameBtnX + renameBtnW;
	}

	private static boolean isAccountUngroupButtonHit(int contentX, int contentW, double mx) {
		int applyBtnW = 18;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int ungroupBtnW = 18;
		int ungroupBtnX = contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - ungroupBtnW - 8;
		return mx >= ungroupBtnX && mx < ungroupBtnX + ungroupBtnW;
	}

	private static boolean isAccountDownArrowHit(int contentX, int contentW, boolean inGroup, double mx) {
		int applyBtnW = 18;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int ungroupBtnW = 18;
		int arrowBtnW = 14;
		int nextBtnX = inGroup
				? contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - ungroupBtnW - 8
				: contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - 6;
		int downArrowX = nextBtnX - arrowBtnW - 2;
		return mx >= downArrowX && mx < downArrowX + arrowBtnW;
	}

	private static boolean isAccountUpArrowHit(int contentX, int contentW, boolean inGroup, double mx) {
		int applyBtnW = 18;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int ungroupBtnW = 18;
		int arrowBtnW = 14;
		int nextBtnX = inGroup
				? contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - ungroupBtnW - 8
				: contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - 6;
		int downArrowX = nextBtnX - arrowBtnW - 2;
		int upArrowX = downArrowX - arrowBtnW - 1;
		return mx >= upArrowX && mx < upArrowX + arrowBtnW;
	}

	private static boolean isAccountDupButtonHit(int contentX, int contentW, boolean inGroup, double mx) {
		int applyBtnW = 18;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int ungroupBtnW = 18;
		int arrowBtnW = 14;
		int dupBtnW = 14;
		int nextBtnX = inGroup
				? contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - ungroupBtnW - 8
				: contentX + contentW - applyBtnW - deleteBtnW - renameBtnW - 6;
		int downArrowX = nextBtnX - arrowBtnW - 2;
		int upArrowX = downArrowX - arrowBtnW - 1;
		int dupBtnX = upArrowX - dupBtnW - 2;
		return mx >= dupBtnX && mx < dupBtnX + dupBtnW;
	}

	private static boolean isGroupDownArrowHit(int contentX, int contentW, boolean hasMoveBtn, double mx) {
		int moveBtnOffset = hasMoveBtn ? 48 : 0;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int arrowBtnW = 14;
		int deleteBtnX = contentX + contentW - moveBtnOffset - deleteBtnW - 2;
		int renameBtnX = deleteBtnX - renameBtnW - 2;
		int downArrowX = renameBtnX - arrowBtnW - 2;
		return mx >= downArrowX && mx < downArrowX + arrowBtnW;
	}

	private static boolean isGroupSortButtonHit(int contentX, int contentW, boolean hasMoveBtn, double mx) {
		int moveBtnOffset = hasMoveBtn ? 48 : 0;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int arrowBtnW = 14;
		int sortBtnW = 14;
		int deleteBtnX = contentX + contentW - moveBtnOffset - deleteBtnW - 2;
		int renameBtnX = deleteBtnX - renameBtnW - 2;
		int downArrowX = renameBtnX - arrowBtnW - 2;
		int upArrowX = downArrowX - arrowBtnW - 1;
		int sortBtnX = upArrowX - sortBtnW - 2;
		return mx >= sortBtnX && mx < sortBtnX + sortBtnW;
	}

	private static boolean isGroupUpArrowHit(int contentX, int contentW, boolean hasMoveBtn, double mx) {
		int moveBtnOffset = hasMoveBtn ? 48 : 0;
		int deleteBtnW = 18;
		int renameBtnW = 18;
		int arrowBtnW = 14;
		int deleteBtnX = contentX + contentW - moveBtnOffset - deleteBtnW - 2;
		int renameBtnX = deleteBtnX - renameBtnW - 2;
		int downArrowX = renameBtnX - arrowBtnW - 2;
		int upArrowX = downArrowX - arrowBtnW - 1;
		return mx >= upArrowX && mx < upArrowX + arrowBtnW;
	}

	static void switchAccount(ModulesScreen screen, Account account) {
		if (account == null) {
			return;
		}

		AccountSwitcher.apply(account);
		screen.accStatus.set("Ник: " + account.getUsername(), 0xFF55EE88);
	}

	static int accListH(ModulesScreen screen) {
		return Math.max(40, PANEL_H - PAD * 2 - ACC_FORM_H);
	}

	/**
	 * Применяет текущий режим сортировки к списку аккаунтов.
	 * Режим ДОБАВЛЕНИЕ возвращает список без изменений (порядок добавления).
	 */
	static List<Account> sortAccounts(List<Account> accounts, ModulesScreenLayout.AccSortMode mode) {
		return switch (mode) {
			case A_Z -> accounts.stream()
					.sorted(Comparator.comparing(a -> a.getUsername().toLowerCase()))
					.toList();
			case Z_A -> accounts.stream()
					.sorted(Comparator.comparing((Account a) -> a.getUsername().toLowerCase()).reversed())
					.toList();
			// В режиме GROUP порядок определяется вручную через moveGroupUp/Down и moveAccountUp/Down,
			// поэтому возвращаем список как есть, не нарушая порядок из AccountManager.
			case GROUP -> accounts;
			default -> accounts;
		};
	}

	/**
	 * Строит плоский список виртуальных строк для режима ГРУППА.
	 * Порядок: сначала именованные группы с аккаунтами, затем пустые именованные группы,
	 * в самом конце — аккаунты без группы (без заголовка).
	 * Свёрнутые группы включают только заголовок — аккаунты пропускаются.
	 */
	static List<Object> buildGroupedRows(List<Account> sorted, Map<String, Boolean> collapsed) {
		List<Object> rows = new ArrayList<>();
		List<Account> ungrouped = new ArrayList<>();

		// Группируем аккаунты по имени группы, сохраняя порядок из sorted
		Map<String, List<Account>> byGroup = new LinkedHashMap<>();

		for (Account a : sorted) {
			String g = a.getGroup();

			if (g.isEmpty()) {
				ungrouped.add(a);
			} else {
				byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(a);
			}
		}

		// Именованные группы в порядке namedGroups — это источник истины для порядка групп
		for (String namedGroup : AccountManager.getInstance().getNamedGroups()) {
			List<Account> members = byGroup.get(namedGroup);

			rows.add(namedGroup);

			if (members == null || collapsed.getOrDefault(namedGroup, false)) {
				continue;
			}

			rows.addAll(members);
		}

		// Группы с аккаунтами, которых нет в namedGroups (на случай рассинхрона)
		Set<String> namedSet = new LinkedHashSet<>(AccountManager.getInstance().getNamedGroups());

		for (Map.Entry<String, List<Account>> entry : byGroup.entrySet()) {
			if (namedSet.contains(entry.getKey())) {
				continue;
			}

			rows.add(entry.getKey());

			if (collapsed.getOrDefault(entry.getKey(), false)) {
				continue;
			}

			rows.addAll(entry.getValue());
		}

		// Аккаунты без группы — в самом конце, с заголовком
		if (ungrouped.isEmpty() == false) {
			rows.add("");

			if (collapsed.getOrDefault("", false) == false) {
				rows.addAll(ungrouped);
			}
		}

		return rows;
	}

	private static boolean handleAccountRightClick(ModulesScreen screen, double mx, double my) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int contentW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int listH = accListH(screen);
		int listContentH = listH - ACC_ENTRY_H;

		if (mx < contentX || mx >= contentX + contentW) {
			return false;
		}

		if (my < listContentY || my >= listContentY + listContentH) {
			return false;
		}

		int rowOffset = (int) (my - listContentY) / ACC_ENTRY_H + screen.accListScroll;
		Account clicked = resolveClickedAccount(screen, rowOffset);

		if (clicked == null) {
			return false;
		}

		cycleAccountGroup(screen, clicked);
		return true;
	}

	/**
	 * Циклически переключает группу аккаунта среди всех существующих групп.
	 * Порядок: все уникальные группы по алфавиту, затем снова первая.
	 */
	private static void cycleAccountGroup(ModulesScreen screen, Account account) {
		AccountManager manager = AccountManager.getInstance();
		TreeSet<String> groups = new TreeSet<>();

		for (Account a : manager.getAccounts()) {
			groups.add(a.getGroup());
		}

		groups.addAll(manager.getNamedGroups());

		List<String> groupList = new ArrayList<>(groups);
		String current = account.getGroup();
		int idx = groupList.indexOf(current);
		String next = groupList.get((idx + 1) % groupList.size());

		account.setGroup(next);
		manager.save();
		screen.accStatus.set("Группа: " + next, 0xFF88CCFF);
	}
}
