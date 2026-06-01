package org.duollectis.client.api.menu.accounts;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.menu.main.ClientScreen;
import org.duollectis.client.api.menu.main.ClientScreenLayout;
import java.util.List;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;
import static org.duollectis.client.utils.MC.mc;

public final class AccountsPanelRenderer {

	private AccountsPanelRenderer() {}

	public static void renderAccountsPanel(ClientScreen screen, DrawContext ctx, int mx, int my) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int contentW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int panelBottom = screen.panelY + PANEL_H - PAD;
		int listH = AccountsPanelActions.accListH(screen);
		int listY = screen.panelY + PAD;
		TextRenderer tr = screen.textRenderer();

		ctx.fill(contentX, listY, contentX + contentW, listY + listH, 0xE0080C12);
		drawBorder(ctx, contentX, listY, contentW, listH, 0xFF2A3A5A);

		renderAccBtn(ctx, tr, screen.accAddAccountBtn, "+A", mx, my);
		renderAccBtn(ctx, tr, screen.accAddGroupBtn, "+G", mx, my);

		boolean groupMode = screen.accSortMode == ClientScreenLayout.AccSortMode.GROUP;
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
			ClientScreen screen,
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
			ClientScreen screen,
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
			ClientScreen screen,
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
		List<Object> rows = AccountsPanelActions.buildGroupedRows(accounts, screen.collapsedGroups);
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
				renderGroupHeader(ctx, tr, screen, groupName, contentX, contentW, ey, mx, my);
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
			ClientScreen screen,
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

		if (screen.accSelected != null && screen.accSelected.getGroup().equals(groupName) == false) {
			int moveBtnW = 46;
			int moveBtnX = contentX + contentW - moveBtnW - 2;
			boolean moveHovered = mx >= moveBtnX && mx < moveBtnX + moveBtnW && my >= btnY && my < btnY + btnH;
			ctx.fill(moveBtnX, btnY, moveBtnX + moveBtnW, btnY + btnH, moveHovered ? 0xFF1A3A5A : 0xFF0D2030);
			ctx.drawTextWithShadow(tr, Text.literal("→ сюда"), moveBtnX + 4, ey + 6, 0xFF88CCFF);
		}

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
			ClientScreen screen,
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
			ClientScreen screen,
			int contentX,
			int contentW
	) {
		Account target = screen.accRenamingAccount;
		boolean isNewAccount = target.getUsername().equals("__new__");

		List<Object> rows = AccountsPanelActions.buildGroupedRows(screen.accCached, screen.collapsedGroups);
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int listH = AccountsPanelActions.accListH(screen);
		int listContentH = listH - ACC_ENTRY_H;

		if (isNewAccount) {
			int fieldY = listContentY + listContentH - ACC_ENTRY_H;
			ctx.fill(contentX + 1, fieldY, contentX + contentW - 1, fieldY + ACC_ENTRY_H, 0xFF0D1A2E);
			drawBorder(ctx, contentX + 1, fieldY, contentW - 2, ACC_ENTRY_H, 0xFF55EE88);

			screen.accAccountRenameField.setX(contentX + 5);
			screen.accAccountRenameField.setY(fieldY + 1);
			screen.accAccountRenameField.setWidth(contentW - 10);
			screen.accAccountRenameField.render(ctx, -1, -1, 0);

			return;
		}

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
			ClientScreen screen,
			int contentX,
			int contentW
	) {
		List<Object> rows = AccountsPanelActions.buildGroupedRows(screen.accCached, screen.collapsedGroups);
		int listContentY = screen.panelY + PAD + ACC_ENTRY_H;
		int listH = AccountsPanelActions.accListH(screen);
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

}
