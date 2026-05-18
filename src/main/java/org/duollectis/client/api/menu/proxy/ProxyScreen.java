package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.proxy.ProxyManager;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;

import java.util.Collections;
import java.util.List;

import static org.duollectis.client.api.menu.proxy.ProxyScreenKeys.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.*;

/**
 * Proxy Manager screen — two-column layout.
 * Left column  : raw list (UNKNOWN/FAILED/CHECKING) + all controls below it
 * Right column : OK list (OK only) + Export/Reset/Recheck buttons below
 * Layout constants/geometry → {@link ProxyScreenLayout}
 * Widget initialisation     → {@link ProxyScreenInit}
 * Per-frame rendering       → {@link ProxyScreenRenderer}
 * List rendering            → {@link ProxyListRenderer}
 * Action handlers           → {@link ProxyScreenActions}
 * Sort logic                → {@link ProxyOkSorter}
 * Translation keys          → {@link ProxyScreenKeys}
 * Shared utilities          → {@link ProxyScreenUtil}
 */
public class ProxyScreen extends Screen {

	// ── scroll offsets ────────────────────────────────────────────────────────
	int rawScroll = 0;
	int okScroll = 0;

	// ── address field colon-split guard ───────────────────────────────────────
	boolean addressSplitting = false;

	// ── double-click detection (left list only) ───────────────────────────────
	int lastRawClickIdx = -1;
	long lastRawClickTime = 0;
	private static final long DOUBLE_CLICK_MS = 400;

	// ── edit mode ─────────────────────────────────────────────────────────────
	Proxy editingProxy = null;

	// ── form fields ───────────────────────────────────────────────────────────
	TextFieldWidget nameField;
	TextFieldWidget addressField;
	TextFieldWidget portField;
	TextFieldWidget usernameField;
	TextFieldWidget passwordField;

	// ── checker settings fields ───────────────────────────────────────────────
	TextFieldWidget maxPingsField;
	TextFieldWidget maxDelayField;
	TextFieldWidget parallelismField;
	TextFieldWidget targetField;
	TextFieldWidget targetPortField;
	ButtonWidget btnPingToggle;

	// ── type dropdown ─────────────────────────────────────────────────────────
	EnumDropdownWidget<Proxy.Type> typeDropdown;

	// ── buttons that need per-frame state updates ─────────────────────────────
	ButtonWidget btnStartStop;
	ButtonWidget btnDelFailed;
	ButtonWidget btnClear;
	ButtonWidget btnResetRaw;
	ButtonWidget btnExportOk;
	ButtonWidget btnResetChk;
	ButtonWidget btnRecheck;
	ButtonWidget btnSort;

	// ── OK list sort mode ─────────────────────────────────────────────────────
	ProxyOkSorter.Mode okSortMode = ProxyOkSorter.Mode.NONE;

	// ── cached sorted lists (rebuilt each frame in render, shared with input) ─
	List<Proxy> cachedRawList = Collections.emptyList();
	List<Proxy> cachedOkList = Collections.emptyList();

	// ── progress bar session tracking (static — survives screen re-opens) ─────
	static int barSessionTotal = -1;
	static int barSessionBaseline = 0;
	static int barSessionDone = -1;

	// ── status message ────────────────────────────────────────────────────────
	String statusMsg = "";
	int statusColor = 0xFFAAAAAA;

	// ── status sink (lambda passed to action helpers) ─────────────────────────
	final ProxyScreenActions.StatusSink statusSink =
			(msg, color) -> {
				statusMsg = msg;
				statusColor = color;
			};

	public ProxyScreen() {
		super(Text.translatable(TITLE));
		ProxyChecker.getInstance().loadSettings();
	}

	// ── geometry shortcuts (delegate to ProxyScreenLayout) ────────────────────

	int lw() {return listW(width);}

	int lx() {return rawListX();}

	int ox() {return okListX(width);}

	int ly() {return listsY();}

	int rlh() {return rawListH(width, height);}

	int olh() {return okListH(height);}

	int cy() {return controlsY(width, height);}

	int mrv() {return maxRawVis(width, height);}

	int mov() {return maxOkVis(height);}

	// ── package-private Screen method wrappers (for ProxyScreenInit) ──────────

	/**
	 * Exposes protected {@code textRenderer} to package-private helpers.
	 */
	TextRenderer tr() {return textRenderer;}

	/**
	 * Exposes protected {@code addDrawableChild} to package-private helpers.
	 */
	<T extends Element & Drawable & Selectable> T reg(T w) {
		return addDrawableChild(w);
	}

	// ── init ──────────────────────────────────────────────────────────────────

	@Override
	protected void init() {
		ProxyScreenInit.formFields(this);
		ProxyScreenInit.checkerSettings(this);
		ProxyScreenInit.buttons(this);
	}

	// ── render ────────────────────────────────────────────────────────────────

	@Override
	public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
		ctx.fill(0, 0, width, height, 0xC0101010);
	}

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		renderBackground(ctx, mx, my, delta);
		ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, PAD, 0xFFFFFF);

		List<Proxy> all = ProxyManager.getInstance().getProxies();
		cachedRawList = all.stream().filter(p -> p.getStatus() != Proxy.Status.OK).toList();
		cachedOkList = ProxyOkSorter.apply(
				all.stream().filter(p -> p.getStatus() == Proxy.Status.OK).toList(), okSortMode);

		ProxyScreenRenderer.renderLists(this, ctx, cachedRawList, cachedOkList, mx, my);
		ProxyScreenRenderer.renderFormLabels(this, ctx);
		ProxyScreenRenderer.renderOverlay(this, ctx, all);
		ProxyScreenRenderer.updateButtonStates(this, all, cachedOkList);

		super.render(ctx, mx, my, delta);
		if (typeDropdown != null)
			typeDropdown.renderOverlay(ctx, mx, my);
	}

	// ── input ─────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(Click event, boolean handled) {
		double mx = event.x(), my = event.y();
		int btn = event.button();

		if ((btn == 0 || btn == 1) && typeDropdown != null) {
			if (typeDropdown.handleClick(mx, my))
				return true;
		}

		int lw = lw(), ly = ly(), rlh = rlh(), olh = olh();

		// left list click
		int rx = lx();
		if (btn == 0 && mx >= rx && mx < rx + lw && my >= ly && my < ly + rlh) {
			List<Proxy> rawList = cachedRawList;
			int idx = (int) (my - ly) / ENTRY_H + rawScroll;
			if (idx >= 0 && idx < rawList.size()) {
				long now = System.currentTimeMillis();
				boolean dbl = (idx == lastRawClickIdx) && (now - lastRawClickTime <= DOUBLE_CLICK_MS);
				lastRawClickIdx = idx;
				lastRawClickTime = now;
				if (dbl)
					startEdit(rawList.get(idx));
				setFocused(null);
				return true;
			}
		}

		// right list click
		int ox = ox();
		if (btn == 0 && mx >= ox && mx < ox + lw && my >= ly && my < ly + olh) {
			ProxyManager pm = ProxyManager.getInstance();
			List<Proxy> okList = cachedOkList;
			int idx = (int) (my - ly) / ENTRY_H + okScroll;
			if (idx >= 0 && idx < okList.size()) {
				Proxy clicked = okList.get(idx);
				String copyLabel = ts(ENTRY_COPY_BTN);
				int copyW = textRenderer.getWidth(copyLabel) + 4;
				int copyX = ox + lw - copyW - 2;
				int entryY = ly + (idx - okScroll) * ENTRY_H;
				if (mx >= copyX && mx < copyX + copyW && my >= entryY && my < entryY + ENTRY_H) {
					String toCopy = clicked.getAddress() + ":" + clicked.getPort();
					client.keyboard.setClipboard(toCopy);
					statusMsg = tsa(MSG_COPIED, toCopy);
					statusColor = 0xFF88FFAA;
					setFocused(null);
					return true;
				}
				pm.setActiveProxy(pm.getActiveProxy() == clicked ? null : clicked);
				setFocused(null);
				return true;
			}
		}

		return super.mouseClicked(event, handled);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEnter()) {
			boolean inSettings = (maxPingsField != null && maxPingsField.isFocused())
					|| (maxDelayField != null && maxDelayField.isFocused())
					|| (parallelismField != null && parallelismField.isFocused());
			if (inSettings) {
				applyCheckerSettings();
				statusMsg = ts(MSG_SETTINGS);
				statusColor = 0xFF88FF88;
				return true;
			}
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
		int lw = lw(), ly = ly(), rlh = rlh(), olh = olh();
		int d = -(int) Math.signum(vAmt);

		int rx = lx();
		if (mouseX >= rx && mouseX < rx + lw && mouseY >= ly && mouseY < ly + rlh) {
			rawScroll = Math.max(0, Math.min(Math.max(0, cachedRawList.size() - mrv()), rawScroll + d));
			return true;
		}
		int ox = ox();
		if (mouseX >= ox && mouseX < ox + lw && mouseY >= ly && mouseY < ly + olh) {
			okScroll = Math.max(0, Math.min(Math.max(0, cachedOkList.size() - mov()), okScroll + d));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
	}

	// ── edit mode ─────────────────────────────────────────────────────────────

	void startEdit(Proxy proxy) {
		editingProxy = proxy;
		clearAndInit();
		statusMsg = ts(MSG_EDITING);
		statusColor = 0xFF88CCFF;
	}

	void saveEdit() {
		ProxyScreenActions.saveEdit(
				editingProxy,
				addressField.getText().trim(),
				portField.getText().trim(),
				nameField.getText().trim(),
				usernameField.getText().trim(),
				passwordField.getText().trim(),
				typeDropdown != null ? typeDropdown.getSelected() : Proxy.Type.SOCKS5,
				statusSink,
				() -> {
					editingProxy = null;
					clearAndInit();
				}
		);
	}

	void cancelEdit() {
		editingProxy = null;
		statusMsg = "";
		clearAndInit();
	}

	// ── form actions ──────────────────────────────────────────────────────────

	void addProxy() {
		ProxyScreenActions.addProxy(
				addressField.getText().trim(),
				portField.getText().trim(),
				nameField.getText().trim(),
				usernameField.getText().trim(),
				passwordField.getText().trim(),
				typeDropdown != null ? typeDropdown.getSelected() : Proxy.Type.SOCKS5
		);
		nameField.setText("");
		addressField.setText("");
		portField.setText("");
		usernameField.setText("");
		passwordField.setText("");
	}

	void removeSelected() {
		ProxyScreenActions.removeProxy(
				editingProxy, () -> {
					editingProxy = null;
					clearAndInit();
				}
		);
	}

	// ── checker actions ───────────────────────────────────────────────────────

	void toggleStartStop() {
		ProxyScreenActions.toggleStartStop(
				this::applyCheckerSettings,
				(total, baseline) -> {
					barSessionTotal = total;
					barSessionBaseline = baseline;
					barSessionDone = -1;
				},
				frozen -> barSessionDone = frozen
		);
	}

	void deleteFailed() {ProxyScreenActions.deleteFailed(statusSink);}

	void clearRaw() {
		ProxyScreenActions.clearRaw(
				editingProxy, statusSink, () -> {
					editingProxy = null;
					rawScroll = 0;
					clearAndInit();
				}
		);
		rawScroll = 0;
	}

	void resetRaw() {ProxyScreenActions.resetRaw(statusSink);}

	void resetChecked() {
		ProxyScreenActions.resetChecked(statusSink);
		okScroll = 0;
	}

	void recheckOk() {ProxyScreenActions.recheckOk(this::applyCheckerSettings, statusSink);}

	void importClipboard() {ProxyScreenActions.importClipboard(client, statusSink);}

	void importFile() {ProxyScreenActions.importFile(statusSink);}

	void exportOk() {ProxyScreenActions.exportOk(statusSink);}

	void applyCheckerSettings() {
		ProxyScreenActions.applyCheckerSettings(
				maxPingsField != null ? maxPingsField.getText() : "",
				maxDelayField != null ? maxDelayField.getText() : "",
				parallelismField != null ? parallelismField.getText() : "",
				targetField != null ? targetField.getText() : "",
				targetPortField != null ? targetPortField.getText() : ""
		);
	}

	// ── sort ──────────────────────────────────────────────────────────────────

	void cycleSort() {
		okSortMode = ProxyOkSorter.cycle(okSortMode);
		okScroll = 0;
	}

	// ── misc ──────────────────────────────────────────────────────────────────

	@Override
	public boolean shouldPause() {return false;}
}
