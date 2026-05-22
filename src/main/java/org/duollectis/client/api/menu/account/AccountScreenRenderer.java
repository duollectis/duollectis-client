package org.duollectis.client.api.menu.account;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.duollectis.client.api.menu.account.AccountScreenLayout.*;
import static org.duollectis.client.api.menu.account.AccountScreenUtil.*;

/** Утилита рендеринга экрана аккаунтов: список с группами, боковая панель и строка статуса. */
final class AccountScreenRenderer {

	/** Ширина зоны стрелок (↑↓) в правой части строки. */
	static final int ARROWS_W = 20;

	private static final String ARROW_UP = "↑";
	private static final String ARROW_DOWN = "↓";
	private static final String ARROW_COLLAPSE_OPEN = "▼";
	private static final String ARROW_COLLAPSE_CLOSED = "▶";

	private AccountScreenRenderer() {}

	/**
	 * Виртуальная строка списка — либо заголовок группы, либо запись аккаунта.
	 * Используется для единого прохода при рендере и обработке кликов.
	 */
	sealed interface ListRow permits ListRow.GroupHeader, ListRow.AccountEntry {

		boolean isGroupHeader();

		String groupName();

		Account account();

		record GroupHeader(String groupName) implements ListRow {

			@Override
			public boolean isGroupHeader() {
				return true;
			}

			@Override
			public Account account() {
				return null;
			}
		}

		record AccountEntry(Account account, String groupName) implements ListRow {

			@Override
			public boolean isGroupHeader() {
				return false;
			}
		}
	}

	/**
	 * Строит плоский список виртуальных строк из аккаунтов и именованных групп.
	 * Аккаунты без группы идут первыми без заголовка. Все именованные группы
	 * всегда присутствуют в списке, даже если в них нет аккаунтов.
	 * Свёрнутые группы включают только заголовок.
	 */
	static List<ListRow> buildRows(
			List<Account> accounts,
			List<String> namedGroups,
			Set<String> collapsed
	) {
		Map<String, List<Account>> grouped = new LinkedHashMap<>();

		grouped.put("", new ArrayList<>());

		for (String g : namedGroups) {
			grouped.put(g, new ArrayList<>());
		}

		for (Account account : accounts) {
			String group = account.getGroup().isBlank() ? "" : account.getGroup();
			grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(account);
		}

		List<ListRow> rows = new ArrayList<>();

		for (Map.Entry<String, List<Account>> entry : grouped.entrySet()) {
			String group = entry.getKey();
			boolean hasHeader = group.isBlank() == false;

			if (hasHeader) {
				rows.add(new ListRow.GroupHeader(group));
			}

			if (hasHeader && collapsed.contains(group)) {
				continue;
			}

			for (Account account : entry.getValue()) {
				rows.add(new ListRow.AccountEntry(account, group));
			}
		}

		return rows;
	}

	static void renderList(
			AccountScreen s,
			DrawContext ctx,
			List<Account> accounts,
			int mx,
			int my
	) {
		int lx = s.lx();
		int ly = s.ly();
		int lw = s.lw();
		int lh = s.lh();

		List<ListRow> rows = buildRows(
				accounts,
				AccountManager.getInstance().getNamedGroups(),
				s.collapsedGroups
		);
		int maxScroll = Math.max(0, rows.size() - s.mv());
		s.listScroll = Math.clamp(s.listScroll, 0, maxScroll);

		ctx.fill(lx, ly, lx + lw, ly + lh, 0xE0080C12);
		drawRect(ctx, lx, ly, lw, lh, 0xFF2A3A5A);

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(
						translatableString("screen.duollectis.account_manager.list.title")
								+ " (" + accounts.size() + ")"
				),
				lx, ly - 10, 0xFF6A7A8A
		);

		int curY = ly;
		int skipped = 0;
		int visibleIndex = 0;

		for (ListRow row : rows) {
			int rowH = row.isGroupHeader() ? GROUP_HEADER_H : ENTRY_H;

			if (skipped < s.listScroll) {
				skipped++;
				continue;
			}

			if (curY + rowH > ly + lh) {
				break;
			}

			if (row.isGroupHeader()) {
				renderGroupHeader(s, ctx, row.groupName(), lx, curY, lw, mx, my);
			} else {
				renderAccountEntry(s, ctx, row.account(), lx, curY, lw, mx, my, visibleIndex);
			}

			curY += rowH;
			visibleIndex++;
		}

		if (rows.size() > s.mv()) {
			int visEnd = Math.min(s.listScroll + s.mv(), rows.size());
			ctx.drawTextWithShadow(
					s.textRenderer(),
					Text.literal((s.listScroll + 1) + "-" + visEnd + "/" + rows.size()),
					lx + 2, ly + lh - 10, 0xFF6A7A8A
			);
		}
	}

	private static void renderGroupHeader(
			AccountScreen s,
			DrawContext ctx,
			String group,
			int lx,
			int ey,
			int lw,
			int mx,
			int my
	) {
		boolean collapsed = s.collapsedGroups.contains(group);
		boolean hovered = mx >= lx && mx < lx + lw && my >= ey && my < ey + GROUP_HEADER_H;

		int bg = hovered ? 0x44334455 : 0x33223344;
		ctx.fill(lx + 1, ey, lx + lw - 1, ey + GROUP_HEADER_H, bg);
		ctx.fill(lx + 1, ey + GROUP_HEADER_H - 1, lx + lw - 1, ey + GROUP_HEADER_H, 0xFF1E2A40);

		int textY = ey + (GROUP_HEADER_H - 8) / 2;

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(group),
				lx + 5, textY, 0xFFAAC8F0
		);

		// Стрелка сворачивания (крайняя правая)
		String collapseArrow = collapsed ? ARROW_COLLAPSE_CLOSED : ARROW_COLLAPSE_OPEN;
		int collapseArrowW = s.textRenderer().getWidth(collapseArrow);
		int collapseX = lx + lw - collapseArrowW - 4;

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(collapseArrow),
				collapseX, textY, 0xFF6A8AAA
		);

		// Стрелки ↑↓ перемещения группы (левее стрелки сворачивания)
		int upX = collapseX - ARROWS_W;
		int downX = upX + 10;

		boolean upHovered = mx >= upX && mx < upX + 9 && my >= ey && my < ey + GROUP_HEADER_H;
		boolean downHovered = mx >= downX && mx < downX + 9 && my >= ey && my < ey + GROUP_HEADER_H;

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(ARROW_UP),
				upX, textY, upHovered ? 0xFFFFFFFF : 0xFF556677
		);

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(ARROW_DOWN),
				downX, textY, downHovered ? 0xFFFFFFFF : 0xFF556677
		);

	}

	private static void renderAccountEntry(
			AccountScreen s,
			DrawContext ctx,
			Account account,
			int lx,
			int ey,
			int lw,
			int mx,
			int my,
			int visibleIndex
	) {
		Account active = AccountManager.getInstance().getActiveAccount();
		boolean isActive = account == active;
		boolean isHovered = mx >= lx && mx < lx + lw && my >= ey && my < ey + ENTRY_H;

		int bg = isActive ? 0xBB0E2818
				: isHovered ? 0x33AACCFF
				: (visibleIndex % 2 == 0 ? 0x18AACCFF : 0x0CAACCFF);

		ctx.fill(lx + 1, ey, lx + lw - 1, ey + ENTRY_H, bg);

		if (isActive) {
			ctx.fill(lx + 1, ey, lx + 3, ey + ENTRY_H, 0xFF55EE88);
		}

		String prefix = isActive ? "► " : "  ";
		int labelColor = isActive ? 0xFF55EE88 : 0xFFBBCCDD;

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(prefix + account.getUsername()),
				lx + 5, ey + 6, labelColor
		);

		// Стрелки ↑↓ перемещения аккаунта (правая часть строки)
		int textY = ey + 6;
		int upX = lx + lw - ARROWS_W - 2;
		int downX = upX + 10;

		boolean upHovered = mx >= upX && mx < upX + 9 && my >= ey && my < ey + ENTRY_H;
		boolean downHovered = mx >= downX && mx < downX + 9 && my >= ey && my < ey + ENTRY_H;

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(ARROW_UP),
				upX, textY, upHovered ? 0xFFFFFFFF : 0xFF445566
		);

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(ARROW_DOWN),
				downX, textY, downHovered ? 0xFFFFFFFF : 0xFF445566
		);
	}

	/**
	 * Вычисляет X-координату начала зоны стрелок ↑↓ в строке заголовка группы.
	 * Используется в {@link AccountScreen} для определения кликнутой зоны.
	 */
	static int groupArrowUpX(int lx, int lw, int collapseArrowW) {
		return lx + lw - collapseArrowW - 4 - ARROWS_W;
	}

	static void renderSidebar(AccountScreen s, DrawContext ctx) {
		int sx = s.sx();
		int sw = s.sw();
		int py = sidebarY();

		drawSectionHeader(
				ctx, s.textRenderer(), sx, py, sw,
				translatableString("screen.duollectis.account_manager.section.add")
		);

		ctx.fill(sx - PAD / 2, sidebarY() - 2, sx - PAD / 2 + 1, s.height - PAD, 0xFF1E2A40);
	}

	static void renderStatus(AccountScreen s, DrawContext ctx) {
		int lx = s.lx();
		int bottomY = s.ly() + s.lh() + PAD;

		if (s.statusMsg.isEmpty() == false) {
			ctx.drawTextWithShadow(
					s.textRenderer(),
					Text.literal(s.statusMsg),
					lx, bottomY, s.statusColor
			);
		}

		bottomY += LABEL_H + PAD;

		Session session = net.minecraft.client.MinecraftClient.getInstance().getSession();
		String currentNick = translatableString("screen.duollectis.account_manager.current")
				+ session.getUsername();

		ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal(currentNick),
				lx, bottomY, 0xFF88CCFF
		);
	}
}
