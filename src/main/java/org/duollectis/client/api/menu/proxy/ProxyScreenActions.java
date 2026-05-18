package org.duollectis.client.api.menu.proxy;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.proxy.ProxyManager;
import org.duollectis.client.utils.AddressParser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.duollectis.client.api.menu.proxy.ProxyScreenKeys.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;

/**
 * All action handlers for the Proxy Manager screen.
 * Methods are package-private and called from {@link ProxyScreen}.
 * Status feedback is written back via the {@link StatusSink} callback.
 */
final class ProxyScreenActions {

	private ProxyScreenActions() {}

	/**
	 * Callback to report status messages back to the screen.
	 */
	interface StatusSink {

		void set(String msg, int color);
	}

	// ── edit mode ─────────────────────────────────────────────────────────────

	static void saveEdit(
			Proxy editingProxy,
			String address, String portStr,
			String name, String username, String password,
			Proxy.Type type,
			StatusSink status,
			Runnable onSuccess
	) {
		if (editingProxy == null)
			return;
		if (address.isEmpty() || portStr.isEmpty()) {
			status.set(ts(MSG_ADDR_REQ), 0xFFFF4444);
			return;
		}
		int port;
		try {
			port = Integer.parseInt(portStr);
		}
		catch (NumberFormatException e) {
			status.set(ts(MSG_BAD_PORT), 0xFFFF4444);
			return;
		}

		editingProxy.setAddress(address);
		editingProxy.setPort(port);
		editingProxy.setName(name.isEmpty() ? address + ":" + port : name);
		editingProxy.setType(type);
		editingProxy.setUsername(username.isEmpty() ? null : username);
		editingProxy.setPassword(password.isEmpty() ? null : password);
		editingProxy.setStatus(Proxy.Status.UNKNOWN);
		editingProxy.setLatency(-1);
		editingProxy.setPingCount(0);

		ProxyManager.getInstance().save();
		status.set(ts(MSG_SAVED), 0xFF44FF88);
		onSuccess.run();
	}

	// ── proxy form ────────────────────────────────────────────────────────────

	static void addProxy(
			String address, String portStr,
			String name, String username, String password,
			Proxy.Type type
	) {
		if (address.isEmpty() || portStr.isEmpty())
			return;
		int port;
		try {
			port = Integer.parseInt(portStr);
		}
		catch (NumberFormatException e) {
			return;
		}

		ProxyManager pm = ProxyManager.getInstance();
		pm.addProxy(new Proxy(
				name.isEmpty() ? address + ":" + port : name,
				address, port, type,
				username.isEmpty() ? null : username,
				password.isEmpty() ? null : password
		));
		pm.save();
	}

	static void removeProxy(Proxy proxy, Runnable onDone) {
		if (proxy == null)
			return;
		ProxyManager pm = ProxyManager.getInstance();
		pm.removeProxy(proxy);
		pm.save();
		onDone.run();
	}

	// ── checker control ───────────────────────────────────────────────────────

	/**
	 * Starts or stops the checker.
	 *
	 * @param onSessionStart called with (total, baseline) when a new session begins
	 * @param onStop         called with (frozenDone) when the checker is stopped
	 */
	static void toggleStartStop(
			Runnable applySettings,
			java.util.function.BiConsumer<Integer, Integer> onSessionStart,
			java.util.function.IntConsumer onStop
	) {
		applySettings.run();
		ProxyChecker checker = ProxyChecker.getInstance();
		List<Proxy> proxies = ProxyManager.getInstance().getProxies();

		if (checker.isRunning()) {
			int frozen = (int) proxies.stream()
			                          .filter(p -> p.getStatus() == Proxy.Status.OK
					                          || p.getStatus() == Proxy.Status.FAILED
					                          || p.getStatus() == Proxy.Status.DEAD_IP
					                          || p.getStatus() == Proxy.Status.DEAD_SERVER
					                          || p.getStatus() == Proxy.Status.PING_ERROR)
			                          .count();
			checker.cancel();
			onStop.accept(frozen);
		}
		else {
			long pending = proxies.stream()
			                      .filter(p -> p.getStatus() == Proxy.Status.UNKNOWN)
			                      .count();
			if (pending > 0) {
				int baseline = (int) proxies.stream()
				                            .filter(p -> p.getStatus() == Proxy.Status.OK
						                            || p.getStatus() == Proxy.Status.FAILED)
				                            .count();
				onSessionStart.accept((int) pending, baseline);
				checker.start(SharedConstants.getProtocolVersion(), proxies);
			}
		}
	}

	// ── list mutations ────────────────────────────────────────────────────────

	static void deleteFailed(StatusSink status) {
		ProxyManager pm = ProxyManager.getInstance();
		List<Proxy> toRemove = pm.getProxies().stream()
		                         .filter(p -> p.getStatus() == Proxy.Status.FAILED)
		                         .toList();
		for (Proxy p : toRemove)
			pm.removeProxy(p);
		pm.save();
		status.set(tsa(MSG_DELETED, toRemove.size()), 0xFFFFAA44);
	}

	static void clearRaw(Proxy editingProxy, StatusSink status, Runnable onEditingRemoved) {
		ProxyManager pm = ProxyManager.getInstance();
		List<Proxy> toRemove = pm.getProxies().stream()
		                         .filter(p -> p.getStatus() == Proxy.Status.UNKNOWN
				                         || p.getStatus() == Proxy.Status.FAILED)
		                         .toList();
		for (Proxy p : toRemove)
			pm.removeProxy(p);
		pm.save();
		if (editingProxy != null && toRemove.contains(editingProxy)) {
			onEditingRemoved.run();
		}
		status.set(tsa(MSG_CLEARED, toRemove.size()), 0xFFFFAA44);
	}

	static void resetRaw(StatusSink status) {
		ProxyManager pm = ProxyManager.getInstance();
		List<Proxy> toReset = pm.getProxies().stream()
		                        .filter(p -> p.getStatus() != Proxy.Status.OK)
		                        .toList();
		for (Proxy p : toReset) {
			p.setStatus(Proxy.Status.UNKNOWN);
			p.setLatency(-1);
			p.setPingCount(0);
		}
		pm.save();
		status.set(tsa(MSG_RESET_RAW, toReset.size()), 0xFFFFAA44);
	}

	static void resetChecked(StatusSink status) {
		ProxyManager pm = ProxyManager.getInstance();
		List<Proxy> toReset = pm.getProxies().stream()
		                        .filter(p -> p.getStatus() == Proxy.Status.OK)
		                        .toList();
		Proxy active = pm.getActiveProxy();
		if (active != null && toReset.contains(active))
			pm.setActiveProxy(null);
		for (Proxy p : toReset) {
			p.setStatus(Proxy.Status.UNKNOWN);
			p.setLatency(-1);
			p.setPingCount(0);
		}
		pm.save();
		status.set(tsa(MSG_RESET_CHK, toReset.size()), 0xFFFFAA44);
	}

	static void recheckOk(Runnable applySettings, StatusSink status) {
		ProxyManager pm = ProxyManager.getInstance();
		List<Proxy> okProxies = pm.getProxies().stream()
		                          .filter(p -> p.getStatus() == Proxy.Status.OK)
		                          .toList();
		if (okProxies.isEmpty()) {
			status.set(ts(MSG_NO_RECHECK), 0xFFFFAA44);
			return;
		}
		for (Proxy p : okProxies) {
			p.setStatus(Proxy.Status.UNKNOWN);
			p.setLatency(-1);
			p.setPingCount(0);
		}
		pm.save();
		applySettings.run();
		ProxyChecker checker = ProxyChecker.getInstance();
		if (!checker.isRunning())
			checker.start(SharedConstants.getProtocolVersion(), okProxies);
		status.set(tsa(MSG_RECHECKING, okProxies.size()), 0xFF88AAFF);
	}

	// ── import / export ───────────────────────────────────────────────────────

	static void importClipboard(MinecraftClient client, StatusSink status) {
		String text = client != null ? client.keyboard.getClipboard() : null;
		if (text == null || text.isBlank()) {
			status.set(ts(MSG_CLIP_EMPTY), 0xFFFFAA44);
			return;
		}
		doImport(text, status);
	}

	static void importFile(StatusSink status) {
		Thread thread = new Thread(
				() -> {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					}
					catch (Exception ignored) {
					}
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle(ts(DLG_IMP_TITLE));
					chooser.setFileFilter(new FileNameExtensionFilter(ts(DLG_IMP_FILTER), "txt", "csv"));
					chooser.setAcceptAllFileFilterUsed(true);
					if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						try {
							doImport(Files.readString(chooser.getSelectedFile().toPath()), status);
						}
						catch (IOException e) {
							status.set(tsa(MSG_ERR_FILE, e.getMessage()), 0xFFFF4444);
						}
					}
				}, "proxy-file-chooser"
		);
		thread.setDaemon(true);
		thread.start();
	}

	static void exportOk(StatusSink status) {
		Thread thread = new Thread(
				() -> {
					try {
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					}
					catch (Exception ignored) {
					}
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogTitle(ts(DLG_EXP_TITLE));
					chooser.setSelectedFile(new File("proxies_ok.txt"));
					chooser.setFileFilter(new FileNameExtensionFilter(ts(DLG_EXP_FILTER), "txt"));
					chooser.setAcceptAllFileFilterUsed(true);
					if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						if (!file.getName().contains("."))
							file = new File(file.getAbsolutePath() + ".txt");
						List<Proxy> okList = ProxyManager.getInstance().getProxies().stream()
						                                 .filter(p -> p.getStatus() == Proxy.Status.OK)
						                                 .toList();
						try (FileWriter fw = new FileWriter(file)) {
							for (Proxy p : okList)
								fw.write(p.getAddress() + ":" + p.getPort() + "\n");
							status.set(tsa(MSG_EXPORTED, okList.size(), file.getName()), 0xFF88AAFF);
						}
						catch (IOException e) {
							status.set(tsa(MSG_ERR_EXPORT, e.getMessage()), 0xFFFF4444);
						}
					}
				}, "proxy-export"
		);
		thread.setDaemon(true);
		thread.start();
	}

	static void doImport(String text, StatusSink status) {
		Set<Proxy> parsed = AddressParser.parseAll(text);
		if (parsed.isEmpty()) {
			status.set(ts(MSG_NO_PROXIES), 0xFFFFAA44);
			return;
		}
		ProxyManager pm = ProxyManager.getInstance();
		pm.addProxies(parsed);
		pm.save();
		status.set(tsa(MSG_IMPORTED, parsed.size()), 0xFF88AAFF);
	}

	// ── checker settings ──────────────────────────────────────────────────────

	static void applyCheckerSettings(
			String maxPingsText,
			String maxDelayText,
			String parallelismText,
			String targetText,
			String targetPortText
	) {
		int maxPings = parseIntOr(maxPingsText, 3);
		int maxDelay = parseIntOr(maxDelayText, 5000);
		int parallelism = parseIntOr(parallelismText, 64);

		ProxyChecker checker = ProxyChecker
				.getInstance()
				.maxPings(maxPings).maxDelayMs(maxDelay).parallelism(parallelism);

		if (targetText != null && !targetText.isBlank() && targetPortText != null && !targetPortText.isBlank()) {
			checker.target(
					targetText.trim(),
					parseIntOr(targetPortText, 25565)
			);
		}
		checker.saveSettings();
	}
}
