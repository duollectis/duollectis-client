package org.duollectis.client.api.menu.modules;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.menu.widget.ConfirmDialog;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.*;

import java.util.List;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;
import static org.duollectis.client.utils.MC.mc;

/**
 * Утилита рендеринга экрана модулей: отрисовывает панель со списком модулей,
 * вкладки, панель настроек или панель аккаунтов.
 */
public final class ModulesScreenRenderer {

	private ModulesScreenRenderer() {}

	public static void renderPanel(
			DrawContext ctx,
			int mx,
			int my,
			int panelX,
			int panelY,
			TextRenderer textRenderer,
			List<Module> modules,
			int listScroll,
			Module selectedModule,
			int settingsScroll,
			ModulesScreen.Tab activeTab,
			Module bindListeningModule
	) {
		ctx.fill(panelX + 3, panelY + 3, panelX + PANEL_W + 3, panelY + PANEL_H + 3, COLOR_SHADOW);
		ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, COLOR_PANEL_BG);
		drawBorder(ctx, panelX, panelY, PANEL_W, PANEL_H, COLOR_PANEL_BORDER);

		int listRight = panelX + LIST_W;
		ctx.fill(panelX, panelY, listRight, panelY + PANEL_H, COLOR_LIST_BG);
		ctx.fill(listRight, panelY, listRight + DIVIDER_W, panelY + PANEL_H, COLOR_DIVIDER);

		renderModuleList(ctx, mx, my, panelX, panelY, textRenderer, modules, listScroll, selectedModule, bindListeningModule);
		renderTabButtons(ctx, mx, my, panelX, panelY, textRenderer, activeTab);

		if (activeTab == ModulesScreen.Tab.MODULES) {
			renderSettingsPanel(ctx, panelX, panelY, textRenderer, selectedModule, settingsScroll);
		}

		renderLangLabel(ctx, panelX, panelY, textRenderer);
	}

	static void renderAccountsPanel(ModulesScreen screen, DrawContext ctx, int mx, int my) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int contentW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int panelBottom = screen.panelY + PANEL_H - PAD;
		int listH = ModulesScreenActions.accListH(screen);
		int listY = screen.panelY + PAD;
		TextRenderer tr = screen.textRenderer();

		ctx.fill(contentX, listY, contentX + contentW, listY + listH, 0xE0080C12);
		drawBorder(ctx, contentX, listY, contentW, listH, 0xFF2A3A5A);

		renderAccBtn(ctx, tr, screen.accAddAccountBtn, "+A", mx, my);
		renderAccBtn(ctx, tr, screen.accAddGroupBtn, "+G", mx, my);

		boolean groupMode = screen.accSortMode == ModulesScreenLayout.AccSortMode.GROUP;
		List<Account> accounts = screen.accCached;
		Account active = AccountManager.getInstance().getActiveAccount();

		renderAccountList(screen, ctx, tr, accounts, active, contentX, contentW, listY, listH, groupMode, mx, my);

		if (screen.accRenamingGroup != null && screen.accGroupRenameField != null) {
			renderGroupRenameField(ctx, tr, screen, contentX, contentW);
		}

		if (screen.accRenamingAccount != null && screen.accAccountRenameField != null) {
			renderAccountRenameField(ctx, screen, contentX, contentW);
		}

		int nickY = panelBottom - 8;
		int statusY = nickY - 8 - ACC_ROW_GAP;

		Session session = mc.getSession();
		ctx.drawTextWithShadow(
				tr,
				Text.literal("Ник: " + session.getUsername()),
				contentX,
				nickY,
				0xFF88CCFF
		);

		if (screen.accStatusMsg.isEmpty() == false) {
			ctx.drawTextWithShadow(tr, Text.literal(screen.accStatusMsg), contentX, statusY, screen.accStatusColor);
		}
	}

	private static void renderAccountList(
			ModulesScreen screen,
			DrawContext ctx,
			TextRenderer tr,
			List<Account> accounts,
			Account active,
			int contentX,
			int contentW,
			int listY,
			int listH,
			boolean groupMode,
			int mx,
			int my
	) {
		// Список начинается ниже кнопки сортировки
		int listContentY = listY + ACC_ENTRY_H;
		int listContentH = listH - ACC_ENTRY_H;
		int maxVis = Math.max(1, listContentH / ACC_ENTRY_H);

		if (groupMode) {
			renderGroupedList(
					screen, ctx, tr, accounts, active,
					contentX, contentW, listContentY, listContentH, maxVis, mx, my
			);
		} else {
			renderFlatList(
					screen, ctx, tr, accounts, active,
					contentX, contentW, listContentY, listContentH, maxVis, mx, my
			);
		}
	}

	private static void renderFlatList(
			ModulesScreen screen,
			DrawContext ctx,
			TextRenderer tr,
			List<Account> accounts,
			Account active,
			int contentX,
			int contentW,
			int listY,
			int listH,
			int maxVis,
			int mx,
			int my
	) {
		screen.accListScroll = Math.clamp(screen.accListScroll, 0, Math.max(0, accounts.size() - maxVis));

		int visCount = Math.min(maxVis, accounts.size() - screen.accListScroll);

		for (int i = 0; i < visCount; i++) {
			int idx = i + screen.accListScroll;
			Account account = accounts.get(idx);
			int ey = listY + i * ACC_ENTRY_H;

			if (ey + ACC_ENTRY_H > listY + listH) {
				break;
			}

			renderAccountRow(ctx, tr, screen, account, active, contentX, contentW, ey, i, mx, my);
		}
	}

	private static void renderGroupedList(
			ModulesScreen screen,
			DrawContext ctx,
			TextRenderer tr,
			List<Account> accounts,
			Account active,
			int contentX,
			int contentW,
			int listY,
			int listH,
			int maxVis,
			int mx,
			int my
	) {
		// Строим плоский список виртуальных строк: заголовок группы + аккаунты
		List<Object> rows = ModulesScreenActions.buildGroupedRows(accounts, screen.collapsedGroups);
		int totalRows = rows.size();

		screen.accListScroll = Math.clamp(screen.accListScroll, 0, Math.max(0, totalRows - maxVis));

		int rowIndex = 0;
		int accountColorIndex = 0;

		for (int i = screen.accListScroll; i < totalRows; i++) {
			int ey = listY + rowIndex * ACC_ENTRY_H;

			if (ey + ACC_ENTRY_H > listY + listH) {
				break;
			}

			Object row = rows.get(i);

			if (row instanceof String groupName) {
				renderGroupHeader(ctx, tr, screen, (String) groupName, contentX, contentW, ey, mx, my);
				accountColorIndex = 0;
			} else {
				renderAccountRow(ctx, tr, screen, (Account) row, active, contentX, contentW, ey, accountColorIndex, mx, my);
				accountColorIndex++;
			}

			rowIndex++;
		}
	}

	private static void renderGroupHeader(
			DrawContext ctx,
			TextRenderer tr,
			ModulesScreen screen,
			String groupName,
			int contentX,
			int contentW,
			int ey,
			int mx,
			int my
	) {
		ctx.fill(contentX + 1, ey, contentX + contentW - 1, ey + ACC_ENTRY_H, 0xFF0A1020);
		ctx.fill(contentX + 1, ey + ACC_ENTRY_H - 1, contentX + contentW - 1, ey + ACC_ENTRY_H, COLOR_GROUP_LINE);

		if (groupName.isEmpty()) {
			ctx.drawTextWithShadow(tr, Text.literal("Без группы"), contentX + 5, ey + 6, 0xFF556677);
			return;
		}

		boolean collapsed = screen.collapsedGroups.getOrDefault(groupName, false);
		String arrow = collapsed ? "▶" : "▼";
		ctx.drawTextWithShadow(tr, Text.literal(arrow + " " + groupName), contentX + 5, ey + 6, COLOR_GROUP_HEADER);

		boolean headerHovered = mx >= contentX && mx < contentX + contentW && my >= ey && my < ey + ACC_ENTRY_H;
		int btnY = ey + 1;
		int btnH = ACC_ENTRY_H - 2;

		// Кнопка "→ сюда" — только если аккаунт выбран и он не в этой группе
		if (screen.accSelected != null && screen.accSelected.getGroup().equals(groupName) == false) {
			int moveBtnW = 46;
			int moveBtnX = contentX + contentW - moveBtnW - 2;
			boolean moveHovered = mx >= moveBtnX && mx < moveBtnX + moveBtnW && my >= btnY && my < btnY + btnH;
			ctx.fill(moveBtnX, btnY, moveBtnX + moveBtnW, btnY + btnH, moveHovered ? 0xFF1A3A5A : 0xFF0D2030);
			ctx.drawTextWithShadow(tr, Text.literal("→ сюда"), moveBtnX + 4, ey + 6, 0xFF88CCFF);
		}

		// Кнопки "✕", "✎" и "↑↓" — только при hover на заголовок
		if (headerHovered) {
			int moveBtnW = screen.accSelected != null && screen.accSelected.getGroup().equals(groupName) == false
					? 46 + 2
					: 0;
			int deleteBtnW = 18;
			int renameBtnW = 18;
			int arrowBtnW = 14;
			int sortBtnW = 14;
			int deleteBtnX = contentX + contentW - moveBtnW - deleteBtnW - 2;
			int renameBtnX = deleteBtnX - renameBtnW - 2;
			int downArrowX = renameBtnX - arrowBtnW - 2;
			int upArrowX = downArrowX - arrowBtnW - 1;
			int sortBtnX = upArrowX - sortBtnW - 2;
	
			boolean deleteHovered = mx >= deleteBtnX && mx < deleteBtnX + deleteBtnW && my >= btnY && my < btnY + btnH;
			boolean renameHovered = mx >= renameBtnX && mx < renameBtnX + renameBtnW && my >= btnY && my < btnY + btnH;
			boolean downHovered = mx >= downArrowX && mx < downArrowX + arrowBtnW && my >= btnY && my < btnY + btnH;
			boolean upHovered = mx >= upArrowX && mx < upArrowX + arrowBtnW && my >= btnY && my < btnY + btnH;
			boolean sortHovered = mx >= sortBtnX && mx < sortBtnX + sortBtnW && my >= btnY && my < btnY + btnH;
	
			ctx.fill(deleteBtnX, btnY, deleteBtnX + deleteBtnW, btnY + btnH, deleteHovered ? 0xFF5A1A1A : 0xFF2A0A0A);
			ctx.drawTextWithShadow(tr, Text.literal("✕"), deleteBtnX + 4, ey + 6, 0xFFFF6666);
	
			ctx.fill(renameBtnX, btnY, renameBtnX + renameBtnW, btnY + btnH, renameHovered ? 0xFF1A3A1A : 0xFF0A2A0A);
			ctx.drawTextWithShadow(tr, Text.literal("✎"), renameBtnX + 4, ey + 6, 0xFF88FF88);
	
			ctx.fill(downArrowX, btnY, downArrowX + arrowBtnW, btnY + btnH, downHovered ? 0xFF2A2A2A : 0xFF141414);
			ctx.drawTextWithShadow(tr, Text.literal("↓"), downArrowX + 3, ey + 6, downHovered ? 0xFFFFFFFF : 0xFF778899);
	
			ctx.fill(upArrowX, btnY, upArrowX + arrowBtnW, btnY + btnH, upHovered ? 0xFF2A2A2A : 0xFF141414);
			ctx.drawTextWithShadow(tr, Text.literal("↑"), upArrowX + 3, ey + 6, upHovered ? 0xFFFFFFFF : 0xFF778899);
	
			ctx.fill(sortBtnX, btnY, sortBtnX + sortBtnW, btnY + btnH, sortHovered ? 0xFF1A2A4A : 0xFF0A1A2A);
			ctx.drawTextWithShadow(tr, Text.literal("⇅"), sortBtnX + 2, ey + 6, sortHovered ? 0xFFAADDFF : 0xFF5588AA);
		}
	}

	private static void renderAccountRow(
			DrawContext ctx,
			TextRenderer tr,
			ModulesScreen screen,
			Account account,
			Account active,
			int contentX,
			int contentW,
			int ey,
			int colorIndex,
			int mx,
			int my
	) {
		boolean isActive = account == active;
		boolean isSelected = account == screen.accSelected;
		boolean isHovered = mx >= contentX && mx < contentX + contentW && my >= ey && my < ey + ACC_ENTRY_H;

		int bg = isSelected ? (isActive ? 0xBB1A3828 : COLOR_ENTRY_SELECTED)
				: isActive ? 0xBB0E2818
				: isHovered ? COLOR_ENTRY_HOVER
				: (colorIndex % 2 == 0 ? 0x18AACCFF : 0x0CAACCFF);

		ctx.fill(contentX + 1, ey, contentX + contentW - 1, ey + ACC_ENTRY_H, bg);

		if (isActive) {
			ctx.fill(contentX + 1, ey, contentX + 3, ey + ACC_ENTRY_H, COLOR_ACTIVE_DOT);
		}

		String prefix = isActive ? "► " : "  ";
		int labelColor = isActive ? COLOR_TEXT_ACTIVE : COLOR_TEXT_PRIMARY;
		ctx.drawTextWithShadow(tr, Text.literal(prefix + account.getUsername()), contentX + 5, ey + 6, labelColor);

		if (isHovered) {
			int btnH = ACC_ENTRY_H - 2;
			int btnY = ey + 1;
			int applyBtnW = 18;
			int deleteBtnW = 18;
			int renameBtnW = 18;
			int ungroupBtnW = 18;
			int arrowBtnW = 14;
			int dupBtnW = 14;
			int applyBtnX = contentX + contentW - applyBtnW - 2;
			int deleteBtnX = applyBtnX - deleteBtnW - 2;
			int renameBtnX = deleteBtnX - renameBtnW - 2;

			boolean applyHov = mx >= applyBtnX && mx < applyBtnX + applyBtnW && my >= btnY && my < btnY + btnH;
			boolean deleteHov = mx >= deleteBtnX && mx < deleteBtnX + deleteBtnW && my >= btnY && my < btnY + btnH;
			boolean renameHov = mx >= renameBtnX && mx < renameBtnX + renameBtnW && my >= btnY && my < btnY + btnH;

			ctx.fill(applyBtnX, btnY, applyBtnX + applyBtnW, btnY + btnH, applyHov ? 0xFF1A4A1A : 0xFF0A2A0A);
			ctx.drawTextWithShadow(tr, Text.literal("▶"), applyBtnX + 4, ey + 6, applyHov ? 0xFF88FF88 : 0xFF44AA44);

			ctx.fill(deleteBtnX, btnY, deleteBtnX + deleteBtnW, btnY + btnH, deleteHov ? 0xFF5A1A1A : 0xFF2A0A0A);
			ctx.drawTextWithShadow(tr, Text.literal("✕"), deleteBtnX + 4, ey + 6, 0xFFFF6666);

			ctx.fill(renameBtnX, btnY, renameBtnX + renameBtnW, btnY + btnH, renameHov ? 0xFF1A3A1A : 0xFF0A2A0A);
			ctx.drawTextWithShadow(tr, Text.literal("✎"), renameBtnX + 4, ey + 6, 0xFF88FF88);

			int nextBtnX = renameBtnX;

			if (account.getGroup().isEmpty() == false) {
				int ungroupBtnX = renameBtnX - ungroupBtnW - 2;
				boolean ungroupHov = mx >= ungroupBtnX && mx < ungroupBtnX + ungroupBtnW && my >= btnY && my < btnY + btnH;
				ctx.fill(ungroupBtnX, btnY, ungroupBtnX + ungroupBtnW, btnY + btnH, ungroupHov ? 0xFF1A2A5A : 0xFF0A1A3A);
				ctx.drawTextWithShadow(tr, Text.literal("−"), ungroupBtnX + 5, ey + 6, 0xFF6699FF);
				nextBtnX = ungroupBtnX;
			}

			int downArrowX = nextBtnX - arrowBtnW - 2;
			int upArrowX = downArrowX - arrowBtnW - 1;
			int dupBtnX = upArrowX - dupBtnW - 2;

			boolean downHov = mx >= downArrowX && mx < downArrowX + arrowBtnW && my >= btnY && my < btnY + btnH;
			boolean upHov = mx >= upArrowX && mx < upArrowX + arrowBtnW && my >= btnY && my < btnY + btnH;
			boolean dupHov = mx >= dupBtnX && mx < dupBtnX + dupBtnW && my >= btnY && my < btnY + btnH;

			ctx.fill(downArrowX, btnY, downArrowX + arrowBtnW, btnY + btnH, downHov ? 0xFF2A2A2A : 0xFF141414);
			ctx.drawTextWithShadow(tr, Text.literal("↓"), downArrowX + 3, ey + 6, downHov ? 0xFFFFFFFF : 0xFF778899);

			ctx.fill(upArrowX, btnY, upArrowX + arrowBtnW, btnY + btnH, upHov ? 0xFF2A2A2A : 0xFF141414);
			ctx.drawTextWithShadow(tr, Text.literal("↑"), upArrowX + 3, ey + 6, upHov ? 0xFFFFFFFF : 0xFF778899);

			ctx.fill(dupBtnX, btnY, dupBtnX + dupBtnW, btnY + btnH, dupHov ? 0xFF2A1A4A : 0xFF150A25);
			ctx.drawTextWithShadow(tr, Text.literal("⧉"), dupBtnX + 2, ey + 6, dupHov ? 0xFFCCAAFF : 0xFF7755AA);

			return;
		}

		String group = account.getGroup();

		if (group.isEmpty()) {
			return;
		}

		int groupTextW = tr.getWidth(group);
		int groupX = contentX + contentW - groupTextW - 5;
		ctx.drawTextWithShadow(tr, Text.literal(group), groupX, ey + 6, COLOR_TEXT_DIM);
	}

	private static void renderAccountRenameField(
			DrawContext ctx,
			ModulesScreen screen,
			int contentX,
			int contentW
	) {
		Account target = screen.accRenamingAccount;
		boolean isNewAccount = target.getUsername().equals("__new__");

		List<Object> rows = ModulesScreenActions.buildGroupedRows(screen.accCached, screen.collapsedGroups);
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int listH = ModulesScreenActions.accListH(screen);
		int listContentH = listH - ACC_ENTRY_H;

		if (isNewAccount) {
			// Поле добавления — в самом низу видимой области
			int fieldY = listContentY + listContentH - ACC_ENTRY_H;
			ctx.fill(contentX + 1, fieldY, contentX + contentW - 1, fieldY + ACC_ENTRY_H, 0xFF0D1A2E);
			drawBorder(ctx, contentX + 1, fieldY, contentW - 2, ACC_ENTRY_H, 0xFF55EE88);

			screen.accAccountRenameField.setX(contentX + 5);
			screen.accAccountRenameField.setY(fieldY + 1);
			screen.accAccountRenameField.setWidth(contentW - 10);
			screen.accAccountRenameField.render(ctx, -1, -1, 0);

			return;
		}

		// Поле переименования — поверх строки аккаунта
		for (int i = screen.accListScroll; i < rows.size(); i++) {
			Object row = rows.get(i);

			if (row instanceof Account acc && acc == target) {
				int rowIndex = i - screen.accListScroll;
				int rowY = listContentY + rowIndex * ACC_ENTRY_H;

				if (rowY + ACC_ENTRY_H > listContentY + listContentH) {
					break;
				}

				ctx.fill(contentX + 1, rowY, contentX + contentW - 1, rowY + ACC_ENTRY_H, 0xFF0D1A2E);
				drawBorder(ctx, contentX + 1, rowY, contentW - 2, ACC_ENTRY_H, 0xFF88FF88);

				screen.accAccountRenameField.setX(contentX + 5);
				screen.accAccountRenameField.setY(rowY + 1);
				screen.accAccountRenameField.setWidth(contentW - 10);
				screen.accAccountRenameField.render(ctx, -1, -1, 0);

				break;
			}
		}
	}

	private static void renderGroupRenameField(
			DrawContext ctx,
			TextRenderer tr,
			ModulesScreen screen,
			int contentX,
			int contentW
	) {
		List<Object> rows = ModulesScreenActions.buildGroupedRows(screen.accCached, screen.collapsedGroups);
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int listH = ModulesScreenActions.accListH(screen);
		int listContentH = listH - ACC_ENTRY_H;

		for (int i = screen.accListScroll; i < rows.size(); i++) {
			Object row = rows.get(i);

			if (row instanceof String groupName && groupName.equals(screen.accRenamingGroup)) {
				int rowIndex = i - screen.accListScroll;
				int headerY = listContentY + rowIndex * ACC_ENTRY_H;

				if (headerY + ACC_ENTRY_H > listContentY + listContentH) {
					break;
				}

				ctx.fill(contentX + 1, headerY, contentX + contentW - 1, headerY + ACC_ENTRY_H, 0xFF0D1A2E);
				drawBorder(ctx, contentX + 1, headerY, contentW - 2, ACC_ENTRY_H, COLOR_GROUP_HEADER);

				screen.accGroupRenameField.setX(contentX + 5);
				screen.accGroupRenameField.setY(headerY + 1);
				screen.accGroupRenameField.setWidth(contentW - 10);
				screen.accGroupRenameField.render(ctx, -1, -1, 0);

				ctx.drawTextWithShadow(
						tr,
						Text.literal("↵ сохранить  Esc отмена"),
						contentX + 5,
						headerY + ACC_ENTRY_H + 2,
						0xFF445566
				);
				break;
			}
		}
	}

	private static void renderAccBtn(
			DrawContext ctx,
			TextRenderer tr,
			int[] btn,
			String label,
			int mx,
			int my
	) {
		int x = btn[0];
		int y = btn[1];
		int w = btn[2];
		int h = btn[3];

		boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
		int bg = hovered ? COLOR_BTN_HOVER : COLOR_BTN_BG;

		ctx.fill(x, y, x + w, y + h, bg);
		drawBorder(ctx, x, y, w, h, COLOR_BTN_BORDER);

		int textX = x + (w - tr.getWidth(label)) / 2;
		int textY = y + (h - 8) / 2;
		ctx.drawTextWithShadow(tr, Text.literal(label), textX, textY, COLOR_BTN_TEXT);
	}

	private static void renderTabButtons(
			DrawContext ctx,
			int mx,
			int my,
			int panelX,
			int panelY,
			TextRenderer textRenderer,
			ModulesScreen.Tab activeTab
	) {
		int tabsY = panelY + PAD + listAreaH() + (TAB_ROW_H - TAB_BTN_H) / 2;
		int tabsX = panelX + PAD;

		renderSingleTab(ctx, mx, my, textRenderer, tabsX, tabsY, "Модули", activeTab == ModulesScreen.Tab.MODULES);
		renderSingleTab(
				ctx, mx, my, textRenderer,
				tabsX + TAB_BTN_W + TAB_BTN_GAP, tabsY,
				"Акки", activeTab == ModulesScreen.Tab.ACCOUNTS
		);
	}

	private static void renderSingleTab(
			DrawContext ctx,
			int mx,
			int my,
			TextRenderer textRenderer,
			int x,
			int y,
			String label,
			boolean active
	) {
		boolean hovered = mx >= x && mx < x + TAB_BTN_W && my >= y && my < y + TAB_BTN_H;

		int bg = active ? COLOR_ENTRY_SELECTED
				: hovered ? COLOR_BTN_HOVER
				: COLOR_BTN_BG;

		ctx.fill(x, y, x + TAB_BTN_W, y + TAB_BTN_H, bg);
		drawBorder(ctx, x, y, TAB_BTN_W, TAB_BTN_H, active ? COLOR_ACTIVE_DOT : COLOR_BTN_BORDER);

		int textColor = active ? COLOR_TEXT_ACTIVE : COLOR_BTN_TEXT;
		int textX = x + (TAB_BTN_W - textRenderer.getWidth(label)) / 2;
		int textY = y + (TAB_BTN_H - 8) / 2;
		ctx.drawTextWithShadow(textRenderer, Text.literal(label), textX, textY, textColor);
	}

	private static void renderModuleList(
			DrawContext ctx,
			int mx,
			int my,
			int panelX,
			int panelY,
			TextRenderer textRenderer,
			List<Module> modules,
			int listScroll,
			Module selectedModule,
			Module bindListeningModule
	) {
		int listRight = panelX + LIST_W;
		int clipTop = panelY + PAD;
		int clipBottom = panelY + PAD + listAreaH();
		int visibleCount = listAreaH() / ENTRY_H;
		int maxScroll = Math.max(0, modules.size() - visibleCount);
		int startIndex = Math.clamp(listScroll, 0, maxScroll);

		ctx.enableScissor(panelX, clipTop, listRight, clipBottom);

		for (int i = startIndex; i < modules.size(); i++) {
			int relativeIndex = i - startIndex;
			int entryY = panelY + PAD + relativeIndex * ENTRY_H;

			if (entryY + ENTRY_H > clipBottom) {
				break;
			}

			Module module = modules.get(i);
			boolean isSelected = module == selectedModule;
			boolean isHovered = mx >= panelX && mx < listRight && my >= entryY && my < entryY + ENTRY_H;
			boolean isActive = module.isActive();

			int bg = isSelected ? COLOR_ENTRY_SELECTED
				: isHovered ? COLOR_ENTRY_HOVER
				: COLOR_ENTRY_NORMAL;

			ctx.fill(panelX, entryY, listRight, entryY + ENTRY_H, bg);

			int dotColor = isActive ? COLOR_ACTIVE_DOT : COLOR_INACTIVE_DOT;
			ctx.fill(panelX + 4, entryY + ENTRY_H / 2 - 2, panelX + 7, entryY + ENTRY_H / 2 + 2, dotColor);

			int nameColor = isActive ? COLOR_TEXT_ACTIVE : COLOR_TEXT_PRIMARY;
			ctx.drawTextWithShadow(
				textRenderer,
				Text.literal(module.getName()),
				panelX + 12,
				entryY + (ENTRY_H - 8) / 2,
				nameColor
			);

			renderBindButton(ctx, textRenderer, module, panelX, entryY, mx, my, bindListeningModule);
		}

		ctx.disableScissor();
	}

	private static void renderBindButton(
			DrawContext ctx,
			TextRenderer textRenderer,
			Module module,
			int panelX,
			int entryY,
			int mx,
			int my,
			Module bindListeningModule
	) {
		int btnX = panelX + LIST_W - BIND_BTN_PAD - BIND_BTN_W;
		int btnY = entryY + (ENTRY_H - BIND_BTN_H) / 2;

		boolean isListening = module == bindListeningModule;
		boolean isHovered = mx >= btnX && mx < btnX + BIND_BTN_W && my >= btnY && my < btnY + BIND_BTN_H;

		int btnBg = isListening ? 0xFF1A3A1A
			: isHovered ? COLOR_BTN_HOVER
			: COLOR_BTN_BG;

		ctx.fill(btnX, btnY, btnX + BIND_BTN_W, btnY + BIND_BTN_H, btnBg);
		drawBorder(ctx, btnX, btnY, BIND_BTN_W, BIND_BTN_H, COLOR_BTN_BORDER);

		String label = isListening
			? "..."
			: (module.getKeyName() == null ? "---" : module.getKeyName());

		int maxLabelW = BIND_BTN_W - 4;
		String trimmed = textRenderer.getWidth(label) <= maxLabelW
			? label
			: trimToWidth(textRenderer, label, maxLabelW);

		int labelColor = isListening ? 0xFF55EE88 : COLOR_TEXT_DIM;
		int labelX = btnX + (BIND_BTN_W - textRenderer.getWidth(trimmed)) / 2;
		int labelY = btnY + (BIND_BTN_H - 8) / 2;

		ctx.drawTextWithShadow(textRenderer, Text.literal(trimmed), labelX, labelY, labelColor);
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

	private static void renderSettingsPanel(
			DrawContext ctx,
			int panelX,
			int panelY,
			TextRenderer textRenderer,
			Module selectedModule,
			int settingsScroll
	) {
		if (selectedModule == null) {
			ctx.drawCenteredTextWithShadow(
				textRenderer,
				Text.literal("Select a module"),
				panelX + LIST_W + DIVIDER_W + (PANEL_W - LIST_W - DIVIDER_W) / 2,
				panelY + PANEL_H / 2,
				COLOR_TEXT_DIM
			);

			return;
		}

		int settingsX = panelX + LIST_W + DIVIDER_W + PAD;
		int settingsW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int clipLeft = panelX + LIST_W + DIVIDER_W;
		int clipRight = panelX + PANEL_W;
		int contentTop = panelY + PAD + ROW_H;
		int panelBottom = panelY + PANEL_H - PAD;

		ctx.drawTextWithShadow(
			textRenderer,
			Text.literal(selectedModule.getName()),
			settingsX,
			panelY + PAD + (ROW_H - 8) / 2,
			COLOR_TITLE
		);

		ctx.enableScissor(clipLeft, contentTop, clipRight, panelBottom);

		int y = contentTop - settingsScroll;

		for (SettingGroup group : selectedModule.getSettings().getGroups()) {
			if (y + ROW_H > contentTop && y < panelBottom) {
				ctx.drawTextWithShadow(
					textRenderer,
					Text.literal(group.id()),
					settingsX,
					y + (ROW_H - 8) / 2,
					COLOR_GROUP_HEADER
				);
				ctx.fill(settingsX, y + ROW_H, settingsX + settingsW, y + ROW_H + 1, COLOR_GROUP_LINE);
			}

			y += ROW_H;

			List<Setting<?>> visibleSettings = group.settings()
				.stream()
				.filter(Setting::isVisible)
				.toList();

			for (int i = 0; i < visibleSettings.size(); i++) {
				Setting<?> setting = visibleSettings.get(i);
				int itemH = itemHeight(textRenderer, setting);

				if (y + itemH > contentTop && y < panelBottom) {
					renderSettingName(ctx, textRenderer, setting, settingsX, settingsW, y);
				}

				y += itemH;

				boolean isLast = i == visibleSettings.size() - 1;

				if (isLast) {
					continue;
				}

				y += SETTING_DIVIDER_PAD;

				if (y > contentTop && y < panelBottom) {
					ctx.fill(settingsX, y, settingsX + settingsW, y + 1, COLOR_SETTING_DIVIDER);
				}

				y += SETTING_DIVIDER_PAD + 1;
			}
		}

		ctx.disableScissor();
	}

	static int itemHeight(TextRenderer textRenderer, Setting<?> setting) {
		if (setting instanceof ItemListSetting itemListSetting) {
			int fieldH = ROW_H;
			int itemRowH = 14;
			int gap = 2;
			List<net.minecraft.item.Item> items = itemListSetting.get();
			int listH = items.isEmpty() ? itemRowH + gap : items.size() * (itemRowH + gap);
			int widgetH = textRenderer.fontHeight + NAME_GAP + fieldH + gap + listH;
			return offsetTop(setting) + widgetH + offsetBottom(setting);
		}

		int widgetH = isInlineWidget(setting)
				? ROW_H
				: textRenderer.fontHeight + nameGap(setting) + ROW_H;
		return offsetTop(setting) + widgetH + offsetBottom(setting);
	}

	static int nameGap(Setting<?> setting) {
		return isSlider(setting) ? SLIDER_NAME_GAP : NAME_GAP;
	}

	static int offsetTop(Setting<?> setting) {
		if (setting instanceof BoolSetting) {
			return BOOL_OFFSET_TOP;
		}

		if (isSlider(setting)) {
			return SLIDER_OFFSET_TOP;
		}

		if (setting instanceof EnumSetting) {
			return ENUM_OFFSET_TOP;
		}

		return FIELD_OFFSET_TOP;
	}

	static int offsetBottom(Setting<?> setting) {
		if (setting instanceof BoolSetting) {
			return BOOL_OFFSET_BOTTOM;
		}

		if (isSlider(setting)) {
			return SLIDER_OFFSET_BOTTOM;
		}

		if (setting instanceof EnumSetting) {
			return ENUM_OFFSET_BOTTOM;
		}

		return FIELD_OFFSET_BOTTOM;
	}


	private static boolean isSlider(Setting<?> setting) {
		return setting instanceof IntSetting intS && intS.isSlider()
			|| setting instanceof DoubleSetting dblS && dblS.isSlider();
	}

	private static boolean isInlineWidget(Setting<?> setting) {
		return setting instanceof BoolSetting || setting instanceof EnumSetting;
	}

	private static boolean isItemList(Setting<?> setting) {
		return setting instanceof ItemListSetting;
	}

	private static void renderSettingName(
			DrawContext ctx,
			TextRenderer textRenderer,
			Setting<?> setting,
			int settingsX,
			int settingsW,
			int y
	) {
		if (setting instanceof BoolSetting) {
			return;
		}

		int top = offsetTop(setting);
		String name = textRenderer.trimToWidth(setting.getName(), settingsW);
		int nameY = setting instanceof EnumSetting
				? y + top + (ROW_H - 8) / 2
				: y + top;

		ctx.drawTextWithShadow(textRenderer, Text.literal(name), settingsX, nameY, COLOR_TEXT_DIM);
	}


	private static void renderLangLabel(
			DrawContext ctx,
			int panelX,
			int panelY,
			TextRenderer textRenderer
	) {
		String langLabel = "Lang: ";
		int langLabelX = panelX + PANEL_W - LANG_FIELD_W - LANG_BTN_W - PAD
			- textRenderer.getWidth(langLabel) - LANG_BTN_GAP;
		int langLabelY = panelY + PANEL_H + PAD + (ROW_H - 8) / 2;
		ctx.drawTextWithShadow(textRenderer, Text.literal(langLabel), langLabelX, langLabelY, COLOR_TEXT_DIM);
	}

	/**
		* Рендерит модальный диалог подтверждения поверх всего экрана.
		* Диалог центрируется по экрану, затемняет фон и показывает кнопки «Да» / «Нет».
		*/
	static void renderConfirmDialog(
			DrawContext ctx,
			TextRenderer tr,
			ConfirmDialog dialog,
			int mx,
			int my,
			int screenW,
			int screenH
	) {
		int dialogW = 200;
		int dialogH = 70;
		int dialogX = (screenW - dialogW) / 2;
		int dialogY = (screenH - dialogH) / 2;

		// Затемнение фона
		ctx.fill(0, 0, screenW, screenH, 0x88000000);

		// Фон диалога
		ctx.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xFF0D1A2E);
		drawBorder(ctx, dialogX, dialogY, dialogW, dialogH, 0xFF2A3A5A);

		// Текст вопроса
		int msgW = tr.getWidth(dialog.message());
		int msgX = dialogX + (dialogW - msgW) / 2;
		ctx.drawTextWithShadow(tr, Text.literal(dialog.message()), msgX, dialogY + 12, 0xFFCCDDEE);

		// Кнопки
		int btnW = 60;
		int btnH = 18;
		int btnY = dialogY + dialogH - btnH - 10;
		int yesBtnX = dialogX + dialogW / 2 - btnW - 5;
		int noBtnX = dialogX + dialogW / 2 + 5;

		boolean yesHov = mx >= yesBtnX && mx < yesBtnX + btnW && my >= btnY && my < btnY + btnH;
		boolean noHov = mx >= noBtnX && mx < noBtnX + btnW && my >= btnY && my < btnY + btnH;

		ctx.fill(yesBtnX, btnY, yesBtnX + btnW, btnY + btnH, yesHov ? 0xFF1A4A1A : 0xFF0A2A0A);
		drawBorder(ctx, yesBtnX, btnY, btnW, btnH, 0xFF55AA55);
		int yesTextX = yesBtnX + (btnW - tr.getWidth("Да")) / 2;
		ctx.drawTextWithShadow(tr, Text.literal("Да"), yesTextX, btnY + 5, 0xFF88FF88);

		ctx.fill(noBtnX, btnY, noBtnX + btnW, btnY + btnH, noHov ? 0xFF4A1A1A : 0xFF2A0A0A);
		drawBorder(ctx, noBtnX, btnY, btnW, btnH, 0xFFAA5555);
		int noTextX = noBtnX + (btnW - tr.getWidth("Нет")) / 2;
		ctx.drawTextWithShadow(tr, Text.literal("Нет"), noTextX, btnY + 5, 0xFFFF8888);
	}

	/**
		* Обрабатывает клик по диалогу подтверждения.
		* Возвращает true если клик попал в область диалога (поглощён), false — промах (закрыть без действия).
		*/
	static boolean handleConfirmDialogClick(ConfirmDialog dialog, double mx, double my, int screenW, int screenH) {
		int dialogW = 200;
		int dialogH = 70;
		int dialogX = (screenW - dialogW) / 2;
		int dialogY = (screenH - dialogH) / 2;

		int btnW = 60;
		int btnH = 18;
		int btnY = dialogY + dialogH - btnH - 10;
		int yesBtnX = dialogX + dialogW / 2 - btnW - 5;

		if (mx >= yesBtnX && mx < yesBtnX + btnW && my >= btnY && my < btnY + btnH) {
			dialog.onConfirm().run();
			return true;
		}

		// Клик в любое место диалога — поглощаем (закрываем без действия)
		return mx >= dialogX && mx < dialogX + dialogW && my >= dialogY && my < dialogY + dialogH;
	}

}
