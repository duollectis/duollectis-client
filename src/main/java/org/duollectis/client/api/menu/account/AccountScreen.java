package org.duollectis.client.api.menu.account;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.duollectis.client.api.menu.account.AccountScreenLayout.*;

/**
 * Главный экран управления оффлайн-аккаунтами. Отображает список сохранённых ников,
 * сгруппированных по полю {@code group}, с возможностью сворачивать/разворачивать группы
 * и перемещать группы и аккаунты стрелками ↑↓.
 */
public class AccountScreen extends Screen {

	int listScroll = 0;

	Account selectedAccount = null;

	TextFieldWidget nicknameField;

	String statusMsg = "";
	int statusColor = 0xFFAAAAAA;

	/** Множество имён групп, которые в данный момент свёрнуты. */
	final Set<String> collapsedGroups = new HashSet<>();

	final AccountScreenActions.StatusSink statusSink = (msg, color) -> {
		statusMsg = msg;
		statusColor = color;
	};

	List<Account> cachedAccounts = Collections.emptyList();

	public AccountScreen() {
		super(Text.translatable("screen.duollectis.account_manager.title"));
	}

	int lx() {
		return listX();
	}

	int ly() {
		return listY();
	}

	int lw() {
		return listW(width);
	}

	int lh() {
		return listH(height);
	}

	int mv() {
		return maxVis(height);
	}

	int sx() {
		return sidebarX(width);
	}

	int sw() {
		return sidebarW();
	}

	TextRenderer textRenderer() {
		return textRenderer;
	}

	<T extends Element & Drawable & Selectable> T register(T widget) {
		return addDrawableChild(widget);
	}

	@Override
	protected void init() {
		AccountScreenInit.widgets(this);
	}

	@Override
	public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
		ctx.fill(0, 0, width, height, 0xC0101010);
	}

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		renderBackground(ctx, mx, my, delta);
		ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, PAD, 0xFFFFFF);

		cachedAccounts = AccountManager.getInstance().getAccounts();

		AccountScreenRenderer.renderList(this, ctx, cachedAccounts, mx, my);
		AccountScreenRenderer.renderSidebar(this, ctx);
		AccountScreenRenderer.renderStatus(this, ctx);

		super.render(ctx, mx, my, delta);
	}

	@Override
	public boolean mouseClicked(Click event, boolean handled) {
		double mx = event.x();
		double my = event.y();
		int btn = event.button();

		if (btn != 0) {
			return super.mouseClicked(event, handled);
		}

		int lx = lx();
		int ly = ly();
		int lw = lw();
		int lh = lh();

		if (mx < lx || mx >= lx + lw || my < ly || my >= ly + lh) {
			return super.mouseClicked(event, handled);
		}

		List<AccountScreenRenderer.ListRow> rows = AccountScreenRenderer.buildRows(
				cachedAccounts,
				AccountManager.getInstance().getNamedGroups(),
				collapsedGroups
		);

		int curY = ly;
		int skipped = 0;

		for (AccountScreenRenderer.ListRow row : rows) {
			int rowH = row.isGroupHeader() ? GROUP_HEADER_H : ENTRY_H;

			if (skipped < listScroll) {
				skipped++;
				continue;
			}

			if (curY + rowH > ly + lh) {
				break;
			}

			if (my < curY || my >= curY + rowH) {
				curY += rowH;
				continue;
			}

			if (row.isGroupHeader()) {
				handleGroupHeaderClick(row.groupName(), mx, lx, lw);
			} else {
				handleAccountEntryClick(row.account(), mx, lx, lw);
			}

			setFocused(null);
			return true;
		}

		return super.mouseClicked(event, handled);
	}

	private void handleGroupHeaderClick(String group, double mx, int lx, int lw) {
		int collapseArrowW = textRenderer().getWidth(collapsedGroups.contains(group) ? "▶" : "▼");
		int collapseX = lx + lw - collapseArrowW - 4;
		int upX = AccountScreenRenderer.groupArrowUpX(lx, lw, collapseArrowW);
		int downX = upX + 10;

		if (mx >= collapseX) {
			if (collapsedGroups.contains(group)) {
				collapsedGroups.remove(group);
			} else {
				collapsedGroups.add(group);
			}
		} else if (mx >= upX && mx < upX + 9) {
			AccountManager.getInstance().moveGroupUp(group);
		} else if (mx >= downX && mx < downX + 9) {
			AccountManager.getInstance().moveGroupDown(group);
		}
	}

	private void handleAccountEntryClick(Account account, double mx, int lx, int lw) {
		int upX = lx + lw - AccountScreenRenderer.ARROWS_W - 2;
		int downX = upX + 10;

		if (mx >= upX && mx < upX + 9) {
			AccountManager.getInstance().moveAccountUp(account);
			return;
		}

		if (mx >= downX && mx < downX + 9) {
			AccountManager.getInstance().moveAccountDown(account);
			return;
		}

		if (account == selectedAccount) {
			AccountScreenActions.switchAccount(account, statusSink);
		} else {
			selectedAccount = account;
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEnter() && nicknameField != null && nicknameField.isFocused()) {
			addAccount();
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
		int lx = lx();
		int ly = ly();
		int lw = lw();
		int lh = lh();

		if (mouseX < lx || mouseX >= lx + lw || mouseY < ly || mouseY >= ly + lh) {
			return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
		}

		int delta = -(int) Math.signum(vAmt);
		List<AccountScreenRenderer.ListRow> rows = AccountScreenRenderer.buildRows(
				cachedAccounts,
				AccountManager.getInstance().getNamedGroups(),
				collapsedGroups
		);
		int maxScroll = Math.max(0, rows.size() - mv());

		listScroll = Math.clamp(listScroll + delta, 0, maxScroll);
		return true;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	void addAccount() {
		if (nicknameField == null) {
			return;
		}

		AccountScreenActions.addAccount(nicknameField.getText(), statusSink);
		nicknameField.setText("");
	}

	void removeSelected() {
		AccountScreenActions.removeAccount(
				selectedAccount,
				statusSink,
				() -> selectedAccount = null
		);
	}

	void switchSelected() {
		AccountScreenActions.switchAccount(selectedAccount, statusSink);
	}

}
