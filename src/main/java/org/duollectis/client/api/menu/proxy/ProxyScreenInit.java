package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;

import static org.duollectis.client.api.menu.proxy.ProxyScreenKeys.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.*;

/**
 * Widget initialisation helpers for {@link ProxyScreen}.
 * <p>All methods receive the screen as a parameter and write directly into its
 * package-private fields. {@code s.tr()} and {@code s.reg()} expose the
 * protected {@code textRenderer} and {@code addDrawableChild} respectively.
 */
final class ProxyScreenInit {

	private ProxyScreenInit() {}

	/**
	 * Creates and registers the proxy form fields (name, address, port, user, pass, type + action buttons).
	 */
	static void formFields(ProxyScreen s) {
		int lw = listW(s.width), lx = rawListX();
		int py = controlsY(s.width, s.height) + SEC_H;

		s.nameField = field(s.tr(), lx, py + LABEL_H, (lw - ROW_GAP) / 2, FLD_NAME);
		s.reg(s.nameField);

		s.addressField = field(
				s.tr(), lx + (lw - ROW_GAP) / 2 + ROW_GAP, py + LABEL_H,
				lw - (lw - ROW_GAP) / 2 - ROW_GAP, FLD_ADDRESS
		);
		s.addressField.setChangedListener(text -> {
			if (s.addressSplitting)
				return;
			int colon = text.indexOf(':');
			if (colon >= 0) {
				s.addressSplitting = true;
				String host = text.substring(0, colon);
				String port = text.substring(colon + 1);
				s.addressField.setText(host);
				if (!port.isEmpty() && port.matches("\\d{1,5}"))
					s.portField.setText(port);
				s.addressSplitting = false;
				s.setFocused(s.portField);
				s.portField.setSelectionEnd(s.portField.getText().length());
			}
		});
		s.reg(s.addressField);

		py += LABEL_H + FIELD_H + ROW_GAP;

		int halfLw = (lw - ROW_GAP) / 2;

		s.portField = field(s.tr(), lx, py + LABEL_H, halfLw, FLD_PORT);
		s.portField.setTextPredicate(v -> v.isEmpty() || v.matches("\\d{0,5}"));
		s.reg(s.portField);

		s.usernameField = field(
				s.tr(), lx + halfLw + ROW_GAP, py + LABEL_H,
				lw - halfLw - ROW_GAP, FLD_USERNAME
		);
		s.reg(s.usernameField);
		py += LABEL_H + FIELD_H + ROW_GAP;

		s.passwordField = field(s.tr(), lx, py + LABEL_H, lw, FLD_PASSWORD);
		s.reg(s.passwordField);
		py += LABEL_H + FIELD_H + ROW_GAP;

		if (s.editingProxy != null) {
			s.nameField.setText(s.editingProxy.getName() != null ? s.editingProxy.getName() : "");
			s.addressField.setText(s.editingProxy.getAddress() != null ? s.editingProxy.getAddress() : "");
			s.portField.setText(s.editingProxy.getPort() > 0 ? String.valueOf(s.editingProxy.getPort()) : "");
			s.usernameField.setText(s.editingProxy.getUsername() != null ? s.editingProxy.getUsername() : "");
			s.passwordField.setText(s.editingProxy.getPassword() != null ? s.editingProxy.getPassword() : "");
		}

		int dropW = 80;
		int btnW = (lw - dropW - ROW_GAP * 2) / 2;
		Proxy.Type prevType = s.editingProxy != null && s.editingProxy.getType() != null
		                      ? s.editingProxy.getType()
		                      : (s.typeDropdown != null ? s.typeDropdown.getSelected() : Proxy.Type.SOCKS5);
		s.typeDropdown = new EnumDropdownWidget<>(
				s.tr(), lx, py, dropW, FIELD_H,
				Proxy.Type.values(), prevType, tp -> {}
		);
		s.reg(s.typeDropdown);

		if (s.editingProxy != null) {
			s.reg(ButtonWidget.builder(t(BTN_SAVE), btn -> s.saveEdit())
			                  .dimensions(lx + dropW + ROW_GAP, py, btnW, FIELD_H).build());
			s.reg(ButtonWidget.builder(t(BTN_CANCEL), btn -> s.cancelEdit())
			                  .dimensions(lx + dropW + ROW_GAP + btnW + ROW_GAP, py, btnW, FIELD_H).build());
		}
		else {
			s.reg(ButtonWidget.builder(t(BTN_ADD), btn -> s.addProxy())
			                  .dimensions(lx + dropW + ROW_GAP, py, btnW, FIELD_H).build());
			s.reg(ButtonWidget.builder(t(BTN_REMOVE), btn -> s.removeSelected())
			                  .dimensions(lx + dropW + ROW_GAP + btnW + ROW_GAP, py, btnW, FIELD_H).build());
		}
	}

	/**
	 * Creates and registers the checker settings fields (pings, delay, threads, target, ping-toggle).
	 */
	static void checkerSettings(ProxyScreen s) {
		int lw = listW(s.width), lx = rawListX();
		int py = controlsY(s.width, s.height) + SEC_H
				+ (LABEL_H + FIELD_H + ROW_GAP) * 3
				+ FIELD_H + ROW_GAP + 8
				+ SEC_H;

		int labelW = s.tr().getWidth(t(LBL_MAX_PINGS)) + 4;
		int labelW2 = s.tr().getWidth(t(LBL_DELAY_MS)) + 4;
		int labelW3 = s.tr().getWidth(t(LBL_THREADS)) + 4;
		int numW = 44;

		ProxyChecker checker = ProxyChecker.getInstance();
		ProxyChecker.Settings settings = checker.getSettings();

		s.maxPingsField = new TextFieldWidget(s.tr(), lx + labelW, py, numW, FIELD_H, Text.empty());
		s.maxPingsField.setMaxLength(3);
		s.maxPingsField.setText(String.valueOf(settings.getMaxPings()));
		s.maxPingsField.setTextPredicate(v -> v.isEmpty() || v.matches("\\d{1,3}"));
		s.reg(s.maxPingsField);

		s.maxDelayField = new TextFieldWidget(
				s.tr(),
				lx + labelW + numW + ROW_GAP + labelW2, py, numW + 10, FIELD_H, Text.empty()
		);
		s.maxDelayField.setMaxLength(6);
		s.maxDelayField.setText(String.valueOf(settings.getMaxDelayMs()));
		s.maxDelayField.setTextPredicate(v -> v.isEmpty() || v.matches("\\d{1,6}"));
		s.reg(s.maxDelayField);
		py += FIELD_H + ROW_GAP;

		s.parallelismField = field(s.tr(), lx + labelW3, py, numW + 10, FLD_THREADS);
		s.parallelismField.setMaxLength(5);
		s.parallelismField.setText(String.valueOf(settings.getParallelism()));
		s.parallelismField.setTextPredicate(v -> v.isEmpty() || v.matches("\\d{1,5}"));
		s.reg(s.parallelismField);
		py += FIELD_H + ROW_GAP + LABEL_H;

		int toggleW = 54;
		int targetWidth = lw - toggleW - ROW_GAP - PORT_W;
		s.targetField = field(s.tr(), lx, py, targetWidth, FLD_ADDRESS);
		s.targetField.setMaxLength(128);
		s.targetField.setText(settings.getTargetHost());
		s.targetField.setChangedListener(text -> {
			if (s.addressSplitting) {
				return;
			}
			int colon = text.indexOf(':');
			if (colon >= 0) {
				s.addressSplitting = true;
				String host = text.substring(0, colon);
				String port = text.substring(colon + 1);
				s.targetField.setText(host);
				if (!port.isEmpty() && port.matches("\\d{1,5}")) {
					s.targetPortField.setText(port);
				}
				s.addressSplitting = false;
				s.setFocused(s.targetPortField);
				s.targetPortField.setSelectionEnd(s.targetPortField.getText().length());
			}
		});
		s.reg(s.targetField);

		s.targetPortField = field(s.tr(), lx + targetWidth, py, PORT_W, TARGET_FLD_PORT);
		s.targetPortField.setText(settings.getTargetPort() + "");
		s.targetPortField.setTextPredicate(v -> v.isEmpty() || v.matches("\\d{0,5}"));
		s.reg(s.targetPortField);

		boolean stage3On = settings.isPingStage3();
		s.btnPingToggle = ButtonWidget
				.builder(
						t(stage3On ? BTN_PING_ON : BTN_PING_OFF),
						btn -> {
							boolean cur = settings.isPingStage3();
							checker.pingStage3(!cur);
							checker.saveSettings();
						}
				)
				.dimensions(lx + lw - toggleW, py, toggleW, FIELD_H).build();
		s.reg(s.btnPingToggle);
	}

	/**
	 * Creates and registers all action buttons (start/stop, import, delete, sort, export, done).
	 */
	static void buttons(ProxyScreen s) {
		int lw = listW(s.width), lx = rawListX(), ox = okListX(s.width);
		int py = controlsY(s.width, s.height) + SEC_H
				+ (LABEL_H + FIELD_H + ROW_GAP) * 3
				+ FIELD_H + ROW_GAP + 8
				+ SEC_H
				+ (FIELD_H + ROW_GAP) * 2
				+ (LABEL_H + FIELD_H + ROW_GAP)
				+ (FIELD_H + ROW_GAP);

		boolean running = ProxyChecker.getInstance().isRunning();
		s.btnStartStop = ButtonWidget.builder(t(running ? BTN_STOP : BTN_START), btn -> s.toggleStartStop())
		                             .dimensions(lx, py, lw, FIELD_H).build();
		s.reg(s.btnStartStop);
		py += FIELD_H + ROW_GAP;

		int qw = (lw - ROW_GAP * 4) / 5;
		s.reg(ButtonWidget.builder(t(BTN_IMPORT_CLIP), btn -> s.importClipboard())
		                  .dimensions(lx, py, qw, FIELD_H).build());
		s.reg(ButtonWidget.builder(t(BTN_IMPORT_FILE), btn -> s.importFile())
		                  .dimensions(lx + qw + ROW_GAP, py, qw, FIELD_H).build());
		s.btnDelFailed = ButtonWidget.builder(t(BTN_DEL_FAILED), btn -> s.deleteFailed())
		                             .dimensions(lx + qw * 2 + ROW_GAP * 2, py, qw, FIELD_H).build();
		s.reg(s.btnDelFailed);
		s.btnClear = ButtonWidget.builder(t(BTN_CLEAR), btn -> s.clearRaw())
		                         .dimensions(lx + qw * 3 + ROW_GAP * 3, py, qw, FIELD_H).build();
		s.reg(s.btnClear);
		s.btnResetRaw = ButtonWidget
				.builder(t(BTN_RESET_RAW), btn -> s.resetRaw())
				.dimensions(lx + qw * 4 + ROW_GAP * 4, py, lw - (qw * 4 + ROW_GAP * 4), FIELD_H)
				.build();
		s.reg(s.btnResetRaw);

		int exportY = listsY() + okListH(s.height) + PAD;
		int tw = (lw - ROW_GAP * 3) / 4;
		s.btnResetChk = ButtonWidget.builder(t(BTN_RESET_CHK), btn -> s.resetChecked())
		                            .dimensions(ox, exportY, tw, OK_BTN_H).build();
		s.reg(s.btnResetChk);
		s.btnRecheck = ButtonWidget.builder(t(BTN_RECHECK), btn -> s.recheckOk())
		                           .dimensions(ox + tw + ROW_GAP, exportY, tw, OK_BTN_H).build();
		s.reg(s.btnRecheck);
		s.btnSort = ButtonWidget.builder(ProxyOkSorter.sortLabel(s.okSortMode), btn -> s.cycleSort())
		                        .dimensions(ox + tw * 2 + ROW_GAP * 2, exportY, tw, OK_BTN_H).build();
		s.reg(s.btnSort);
		s.btnExportOk = ButtonWidget
				.builder(t(BTN_EXPORT_OK), btn -> s.exportOk())
				.dimensions(ox + tw * 3 + ROW_GAP * 3, exportY, lw - tw * 3 - ROW_GAP * 3, OK_BTN_H)
				.build();
		s.reg(s.btnExportOk);

		s.reg(ButtonWidget.builder(t(BTN_DONE), btn -> s.close())
		                  .dimensions(s.width / 2 - 50, s.height - PAD - 20, 100, 20).build());
	}
}
