package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;

import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;

/**
 * Инициализатор виджетов экрана прокси: создаёт выпадающие списки фильтра и сортировки,
 * поля формы редактирования, настройки чекера и кнопки управления.
 */
final class ProxyScreenInit {

	private ProxyScreenInit() {}

	static void filterDropdown(ProxyScreen s) {
		int dropW = 100;
		int sortW = 80;
		int lw = listW(s.width);
		int dropX = listX() + lw - dropW;
		int sortX = dropX - ROW_GAP - sortW;
		int rowY = listY() - FIELD_H - 2;

		ProxyFilter initFilter = s.filterDropdown != null
				? s.filterDropdown.getSelected()
				: (s.savedFilter != null ? s.savedFilter : ProxyFilter.ALL);

		s.filterDropdown = s.register(new EnumDropdownWidget<>(
				s.textRenderer(),
				dropX,
				rowY,
				dropW,
				FIELD_H,
				ProxyFilter.values(),
				initFilter,
				filter -> {
					s.listScroll = 0;
					s.saveUiSettings();
				}
		));

		SortMode initSort = s.sortDropdown != null
				? s.sortDropdown.getSelected()
				: (s.savedSort != null ? s.savedSort : SortMode.NONE);

		s.sortDropdown = s.register(new EnumDropdownWidget<>(
				s.textRenderer(),
				sortX,
				rowY,
				sortW,
				FIELD_H,
				SortMode.values(),
				initSort,
				sorter -> {
					s.listScroll = 0;
					s.saveUiSettings();
				}
		));
	}

	static void formFields(ProxyScreen s) {
		int sw = sidebarW();
		int portW = (sw - ROW_GAP) / 2;
		int addrW = sw - portW - ROW_GAP;
		RowLayout layout = new RowLayout(sidebarX(s.width), sidebarY() + SEC_H, sw);

		layout.label().next();
		s.nameField = s.register(field(
				s.textRenderer(),
				layout.x(),
				layout.y(),
				sw,
				"screen.duollectis.proxy_manager.field.name"
		));
		layout.rowDone();

		layout.label();
		int[] addrRow = layout.split(addrW, portW);
		s.addressField = s.register(field(
				s.textRenderer(),
				addrRow[0],
				layout.y(),
				addrW,
				"screen.duollectis.proxy_manager.field.address"
		));
		s.addressField.setChangedListener(text -> colonSplit(s, text, s.addressField, s.portField));
		s.portField = s.register(field(
				s.textRenderer(),
				addrRow[1],
				layout.y(),
				portW,
				"screen.duollectis.proxy_manager.field.port"
		));
		s.portField.setTextPredicate(v -> v.isEmpty() || v.matches("\\d{0,5}"));
		layout.rowDone();

		layout.label();
		int[] credRow = layout.split(addrW, portW);
		s.usernameField = s.register(field(
				s.textRenderer(),
				credRow[0],
				layout.y(),
				addrW,
				"screen.duollectis.proxy_manager.field.username"
		));
		s.passwordField = s.register(field(
				s.textRenderer(),
				credRow[1],
				layout.y(),
				portW,
				"screen.duollectis.proxy_manager.field.password"
		));
		layout.rowDone();

		if (s.editingProxy != null) {
			s.nameField.setText(s.editingProxy.getName() != null ? s.editingProxy.getName() : "");
			s.addressField.setText(s.editingProxy.getAddress() != null ? s.editingProxy.getAddress() : "");
			s.portField.setText(s.editingProxy.getPort() > 0 ? String.valueOf(s.editingProxy.getPort()) : "");
			s.usernameField.setText(s.editingProxy.getUsername() != null ? s.editingProxy.getUsername() : "");
			s.passwordField.setText(s.editingProxy.getPassword() != null ? s.editingProxy.getPassword() : "");
		}

		int typeDropdownW = 70;
		int actionBtnW = (sw - typeDropdownW - ROW_GAP * 2) / 2;
		int[] actionRow = layout.split(typeDropdownW, actionBtnW, -1);

		s.typeDropdown = s.register(new EnumDropdownWidget<>(
				s.textRenderer(),
				actionRow[0],
				layout.y(),
				typeDropdownW,
				FIELD_H,
				Proxy.Type.values(),
				resolveInitialType(s),
				tp -> {}
		));

		if (s.editingProxy == null) {
			s.register(btn(
					"screen.duollectis.proxy_manager.btn.add",
					actionRow[1],
					layout.y(),
					actionBtnW,
					b -> s.addProxy()
			));
			s.register(btn(
					"screen.duollectis.proxy_manager.btn.remove",
					actionRow[2],
					layout.y(),
					actionBtnW,
					b -> s.removeSelected()
			));
		} else {
			s.register(btn(
					"screen.duollectis.proxy_manager.btn.save",
					actionRow[1],
					layout.y(),
					actionBtnW,
					b -> s.saveEdit()
			));
			s.register(btn(
					"screen.duollectis.proxy_manager.btn.cancel",
					actionRow[2],
					layout.y(),
					actionBtnW,
					b -> s.cancelEdit()
			));
		}
	}

	static void checkerSettings(ProxyScreen screen) {
		int sw = sidebarW();
		RowLayout layout = new RowLayout(sidebarX(screen.width), sidebarY() + FORM_H + SEC_H, sw);

		ProxyChecker checker = ProxyChecker.getInstance();
		ProxyChecker.Settings settings = checker.getSettings();

		int maxPingsLabelW = screen.textRenderer()
				.getWidth(translatable("screen.duollectis.proxy_manager.label.max_pings")) + 4;
		int delayLabelW = screen.textRenderer()
				.getWidth(translatable("screen.duollectis.proxy_manager.label.delay_ms")) + 4;
		int delayFieldW = sw - maxPingsLabelW - NUM_W - delayLabelW - ROW_GAP * 3;
		int[] pingDelayRow = layout.split(maxPingsLabelW, NUM_W, delayLabelW, delayFieldW);

		screen.maxPingsField = screen.register(numericField(
				screen.textRenderer(),
				pingDelayRow[1],
				layout.y(),
				NUM_W,
				3,
				String.valueOf(settings.getMaxPings()),
				"\\d{1,3}"
		));

		screen.maxDelayField = screen.register(numericField(
				screen.textRenderer(),
				pingDelayRow[3],
				layout.y(),
				delayFieldW,
				6,
				String.valueOf(settings.getMaxDelayMs()),
				"\\d{1,6}"
		));
		layout.rowDone();

		int threadsLabelW = screen.textRenderer()
				.getWidth(translatable("screen.duollectis.proxy_manager.label.threads")) + 4;
		int threadsFieldW = sw - threadsLabelW - ROW_GAP;
		int[] threadsRow = layout.split(threadsLabelW, threadsFieldW);

		screen.parallelismField = screen.register(numericField(
				screen.textRenderer(),
				threadsRow[1],
				layout.y(),
				threadsFieldW,
				5,
				String.valueOf(settings.getParallelism()),
				"\\d{1,5}"
		));
		screen.parallelismField.setPlaceholder(translatable("screen.duollectis.proxy_manager.field.threads"));
		layout.rowDone().label();

		int pingToggleW = 50;
		int targetPortW = PORT_W;
		int targetFieldW = sw - pingToggleW - targetPortW - ROW_GAP * 2;
		int[] targetRow = layout.split(targetFieldW, targetPortW, pingToggleW);

		screen.targetField = screen.register(field(
				screen.textRenderer(),
				targetRow[0],
				layout.y(),
				targetFieldW,
				"screen.duollectis.proxy_manager.field.address"
		));
		screen.targetField.setMaxLength(128);
		screen.targetField.setText(settings.getTargetHost());
		screen.targetField.setChangedListener(
				text -> colonSplit(screen, text, screen.targetField, screen.targetPortField)
		);

		screen.targetPortField = screen.register(numericField(
				screen.textRenderer(),
				targetRow[1],
				layout.y(),
				targetPortW,
				5,
				String.valueOf(settings.getTargetPort()),
				"\\d{0,5}"
		));

		boolean pingEnabled = settings.isPingStage3();
		screen.btnPingToggle = screen.register(btn(
				pingEnabled
						? "screen.duollectis.proxy_manager.btn.ping_on"
						: "screen.duollectis.proxy_manager.btn.ping_off",
				targetRow[2],
				layout.y(),
				pingToggleW,
				b -> {
					checker.pingStage3(!settings.isPingStage3());
					checker.saveSettings();
				}
		));
	}

	static void buttons(ProxyScreen s) {
		int sw = sidebarW();
		RowLayout layout = new RowLayout(sidebarX(s.width), sidebarY() + FORM_H + CHECKER_H, sw);

		boolean running = ProxyChecker.getInstance().isRunning();
		layout.next();
		s.btnStartStop = s.register(btn(
				running
						? "screen.duollectis.proxy_manager.btn.stop"
						: "screen.duollectis.proxy_manager.btn.start",
				layout.x(),
				layout.y(),
				sw,
				b -> s.toggleStartStop()
		));
		layout.rowDone();

		int[] importRow = layout.splitEqual(2);
		s.register(btn(
				"screen.duollectis.proxy_manager.btn.import_clip",
				importRow[0],
				layout.y(),
				layout.slotW(2),
				b -> s.importClipboard()
		));
		s.register(btn(
				"screen.duollectis.proxy_manager.btn.import_file",
				importRow[1],
				layout.y(),
				layout.slotW(2),
				b -> s.importFile()
		));
		layout.rowDone();

		int[] manageRow = layout.splitEqual(3);
		s.btnDelFailed = s.register(btn(
				"screen.duollectis.proxy_manager.btn.del_failed",
				manageRow[0],
				layout.y(),
				layout.slotW(3),
				b -> s.deleteFailed()
		));
		s.btnClear = s.register(btn(
				"screen.duollectis.proxy_manager.btn.clear",
				manageRow[1],
				layout.y(),
				layout.slotW(3),
				b -> s.clearRaw()
		));
		s.btnResetRaw = s.register(btn(
				"screen.duollectis.proxy_manager.btn.reset_raw",
				manageRow[2],
				layout.y(),
				layout.slotW(3),
				b -> s.resetRaw()
		));
		layout.rowDone();

		int[] exportRow = layout.splitEqual(2);
		s.btnResetChk = s.register(btn(
				"screen.duollectis.proxy_manager.btn.reset_checked",
				exportRow[0],
				layout.y(),
				layout.slotW(2),
				b -> s.resetChecked()
		));
		s.btnExportOk = s.register(btn(
				"screen.duollectis.proxy_manager.btn.export_ok",
				exportRow[1],
				layout.y(),
				layout.slotW(2),
				b -> s.exportOk()
		));
		layout.rowDone();

		int[] nextRow = layout.splitEqual(1);
		s.btnNext = s.register(btn(
				"screen.duollectis.proxy_manager.btn.next",
				nextRow[0],
				layout.y(),
				layout.slotW(1),
				b -> s.nextProxy()
		));
	}

	private static ButtonWidget btn(
			String key,
			int x,
			int y,
			int w,
			ButtonWidget.PressAction action
	) {
		return ButtonWidget.builder(translatable(key), action)
				.dimensions(x, y, w, FIELD_H)
				.build();
	}

	private static void colonSplit(
			ProxyScreen s,
			String text,
			TextFieldWidget hostField,
			TextFieldWidget portField
	) {
		if (s.addressSplitting) {
			return;
		}

		int colon = text.indexOf(':');

		if (colon < 0) {
			return;
		}

		s.addressSplitting = true;
		hostField.setText(text.substring(0, colon));
		String port = text.substring(colon + 1);

		if (!port.isEmpty() && port.matches("\\d{1,5}")) {
			portField.setText(port);
		}

		s.addressSplitting = false;
		s.setFocused(portField);
		portField.setSelectionEnd(portField.getText().length());
	}

	private static Proxy.Type resolveInitialType(ProxyScreen s) {
		if (s.editingProxy != null && s.editingProxy.getType() != null) {
			return s.editingProxy.getType();
		}

		return s.typeDropdown != null ? s.typeDropdown.getSelected() : Proxy.Type.SOCKS5;
	}
}
