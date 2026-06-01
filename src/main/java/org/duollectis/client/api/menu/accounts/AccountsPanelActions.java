package org.duollectis.client.api.menu.accounts;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.account.AccountSwitcher;
import org.duollectis.client.api.menu.main.ClientScreen;
import org.duollectis.client.api.menu.main.ClientScreenLayout;
import org.duollectis.client.api.menu.widget.ConfirmDialog;

import java.util.*;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;

/**
 * Утилита обработки пользовательских действий на панели аккаунтов:
 * клики по группам, строкам аккаунтов, переименование, сортировка.
 */
public final class AccountsPanelActions {

	@FunctionalInterface
	public interface AccStatusSink {
		void set(String message, int color);
	}

	private AccountsPanelActions() {}

	public static boolean handleAccountsPanelClick(ClientScreen screen, double mx, double my, int btn) {
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
			ClientScreen screen,
			int rowOffset,
			int contentX,
			int contentW,
			int listContentY,
			double mx,
			double my
	) {
		if (screen.accSortMode != ClientScreenLayout.AccSortMode.GROUP) {
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

			if (hasMoveBtn && isMoveButtonHit(contentX, contentW, mx)) {
				moveAccountToGroup(screen, screen.accSelected, groupName);
				return true;
			}

			if (isDeleteButtonHit(contentX, contentW, hasMoveBtn, mx)) {
				screen.confirmDialog = new ConfirmDialog(
					"Удалить группу «" + groupName + "»?",
					() -> deleteGroup(screen, groupName)
				);
				return true;
			}

			if (isRenameButtonHit(contentX, contentW, hasMoveBtn, mx)) {
				startGroupRename(screen, groupName, contentX, contentW, headerY);
				return true;
			}

			if (isGroupDownArrowHit(contentX, contentW, hasMoveBtn, mx)) {
				AccountManager.getInstance().moveGroupDown(groupName);
				return true;
			}

			if (isGroupUpArrowHit(contentX, contentW, hasMoveBtn, mx)) {
				AccountManager.getInstance().moveGroupUp(groupName);
				return true;
			}

			if (isGroupSortButtonHit(contentX, contentW, hasMoveBtn, mx)) {
				AccountManager.getInstance().sortGroupAccounts(groupName);
				screen.accStatus.set("Группа «" + groupName + "» отсортирована", 0xFF88CCFF);
				return true;
			}

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

	private static void moveAccountToGroup(ClientScreen screen, Account account, String groupName) {
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

	public static void deleteGroup(ClientScreen screen, String groupName) {
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
	public static void startGroupRename(ClientScreen screen, String groupName, int contentX, int contentW, int headerY) {
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
	public static void commitGroupRename(ClientScreen screen) {
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

	public static void cancelGroupRename(ClientScreen screen) {
		screen.accRenamingGroup = null;
		screen.accGroupRenameField = null;
		screen.setFocused(null);
	}

	public static void addGroup(ClientScreen screen) {
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

	public static Account resolveClickedAccount(ClientScreen screen, int rowOffset) {
		if (screen.accSortMode != ClientScreenLayout.AccSortMode.GROUP) {
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

	public static boolean handleAccountsScroll(ClientScreen screen, double mx, double my, double vAmt) {
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
	public static void startAddAccount(ClientScreen screen) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int contentW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int listH = accListH(screen);
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int listContentH = listH - ACC_ENTRY_H;
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
	public static void commitAccountRename(ClientScreen screen) {
		if (screen.accRenamingAccount == null || screen.accAccountRenameField == null) {
			return;
		}

		String newName = screen.accAccountRenameField.getText().trim();
		AccountManager manager = AccountManager.getInstance();

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

	public static void cancelAccountRename(ClientScreen screen) {
		screen.accRenamingAccount = null;
		screen.accAccountRenameField = null;
		screen.setFocused(null);
	}

	/**
	 * Обрабатывает клик по кнопкам "✕" и "✎" на строке аккаунта.
	 */
	private static boolean handleAccountRowButtonClick(
			ClientScreen screen,
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

	public static void startAccountRename(ClientScreen screen, Account account, int contentX, int contentW, int rowY) {
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

	public static void switchAccount(ClientScreen screen, Account account) {
		if (account == null) {
			return;
		}

		AccountSwitcher.apply(account);
		screen.accStatus.set("Ник: " + account.getUsername(), 0xFF55EE88);
	}

	public static int accListH(ClientScreen screen) {
		return Math.max(40, PANEL_H - PAD * 2 - ACC_FORM_H);
	}

	/**
	 * Применяет текущий режим сортировки к списку аккаунтов.
	 * Режим GROUP возвращает список без изменений (порядок определяется вручную).
	 */
	public static List<Account> sortAccounts(List<Account> accounts, ClientScreenLayout.AccSortMode mode) {
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
	public static List<Object> buildGroupedRows(List<Account> sorted, Map<String, Boolean> collapsed) {
		List<Object> rows = new ArrayList<>();
		List<Account> ungrouped = new ArrayList<>();

		Map<String, List<Account>> byGroup = new LinkedHashMap<>();

		for (Account a : sorted) {
			String g = a.getGroup();

			if (g.isEmpty()) {
				ungrouped.add(a);
			} else {
				byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(a);
			}
		}

		for (String namedGroup : AccountManager.getInstance().getNamedGroups()) {
			List<Account> members = byGroup.get(namedGroup);

			rows.add(namedGroup);

			if (members == null || collapsed.getOrDefault(namedGroup, false)) {
				continue;
			}

			rows.addAll(members);
		}

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

		if (ungrouped.isEmpty() == false) {
			rows.add("");

			if (collapsed.getOrDefault("", false) == false) {
				rows.addAll(ungrouped);
			}
		}

		return rows;
	}

	private static boolean handleAccountRightClick(ClientScreen screen, double mx, double my) {
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

	private static void cycleAccountGroup(ClientScreen screen, Account account) {
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