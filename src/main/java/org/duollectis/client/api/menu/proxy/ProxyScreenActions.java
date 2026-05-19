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
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;

final class ProxyScreenActions {

    private ProxyScreenActions() {}

    static void saveEdit(
            Proxy editingProxy,
            String address, String portStr,
            String name, String username, String password,
            Proxy.Type type,
            StatusSink status,
            Runnable onSuccess
    ) {
        if (editingProxy == null) {
            return;
        }

        if (address.isEmpty() || portStr.isEmpty()) {
            status.set(translatableString("screen.duollectis.proxy_manager.msg.address_port_required"), 0xFFFF4444);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            status.set(translatableString("screen.duollectis.proxy_manager.msg.invalid_port"), 0xFFFF4444);
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
        status.set(translatableString("screen.duollectis.proxy_manager.msg.proxy_saved"), 0xFF44FF88);
        onSuccess.run();
    }

    static void addProxy(
            String address, String portStr,
            String name, String username, String password,
            Proxy.Type type
    ) {
        if (address.isEmpty() || portStr.isEmpty()) {
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
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
        if (proxy == null) {
            return;
        }

        ProxyManager pm = ProxyManager.getInstance();
        pm.removeProxy(proxy);
        pm.save();
        onDone.run();
    }

    static void toggleStartStop(
            Runnable applySettings,
            BiConsumer<Integer, Integer> onSessionStart,
            IntConsumer onStop
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
        } else {
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

    static void deleteFailed(StatusSink status) {
        ProxyManager pm = ProxyManager.getInstance();
        List<Proxy> toRemove = pm.getProxies().stream()
                .filter(p -> p.getStatus() == Proxy.Status.FAILED)
                .toList();

        for (Proxy p : toRemove) {
            pm.removeProxy(p);
        }

        pm.save();
        status.set(tsa("screen.duollectis.proxy_manager.msg.deleted_failed", toRemove.size()), 0xFFFFAA44);
    }

    static void clearRaw(Proxy editingProxy, StatusSink status, Runnable onEditingRemoved) {
        ProxyManager pm = ProxyManager.getInstance();
        List<Proxy> toRemove = pm.getProxies().stream()
                .filter(p -> p.getStatus() == Proxy.Status.UNKNOWN
                        || p.getStatus() == Proxy.Status.FAILED)
                .toList();

        for (Proxy p : toRemove) {
            pm.removeProxy(p);
        }

        pm.save();

        if (editingProxy != null && toRemove.contains(editingProxy)) {
            onEditingRemoved.run();
        }

        status.set(tsa("screen.duollectis.proxy_manager.msg.cleared_raw", toRemove.size()), 0xFFFFAA44);
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
        status.set(tsa("screen.duollectis.proxy_manager.msg.reset_raw", toReset.size()), 0xFFFFAA44);
    }

    static void resetChecked(StatusSink status) {
        ProxyManager pm = ProxyManager.getInstance();
        List<Proxy> toReset = pm.getProxies().stream()
                .filter(p -> p.getStatus() == Proxy.Status.OK)
                .toList();

        Proxy active = pm.getActiveProxy();

        if (active != null && toReset.contains(active)) {
            pm.setActiveProxy(null);
        }

        for (Proxy p : toReset) {
            p.setStatus(Proxy.Status.UNKNOWN);
            p.setLatency(-1);
            p.setPingCount(0);
        }

        pm.save();
        status.set(tsa("screen.duollectis.proxy_manager.msg.reset_checked", toReset.size()), 0xFFFFAA44);
    }

    static void importClipboard(MinecraftClient client, StatusSink status) {
        String text = client != null ? client.keyboard.getClipboard() : null;

        if (text == null || text.isBlank()) {
            status.set(translatableString("screen.duollectis.proxy_manager.msg.clipboard_empty"), 0xFFFFAA44);
            return;
        }

        doImport(text, status);
    }

    static void importFile(StatusSink status) {
        daemonThread("proxy-file-chooser", () -> {
            applySystemLookAndFeel();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(translatableString("screen.duollectis.proxy_manager.dialog.import_title"));
            chooser.setFileFilter(new FileNameExtensionFilter(
                    translatableString("screen.duollectis.proxy_manager.dialog.import_filter"), "txt", "csv"));
            chooser.setAcceptAllFileFilterUsed(true);

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                try {
                    doImport(Files.readString(chooser.getSelectedFile().toPath()), status);
                } catch (IOException e) {
                    status.set(tsa("screen.duollectis.proxy_manager.msg.error_reading_file", e.getMessage()), 0xFFFF4444);
                }
            }
        });
    }

    static void exportOk(StatusSink status) {
        daemonThread("proxy-export", () -> {
            applySystemLookAndFeel();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(translatableString("screen.duollectis.proxy_manager.dialog.export_title"));
            chooser.setSelectedFile(new File("proxies_ok.txt"));
            chooser.setFileFilter(new FileNameExtensionFilter(
                    translatableString("screen.duollectis.proxy_manager.dialog.export_filter"), "txt"));
            chooser.setAcceptAllFileFilterUsed(true);

            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();

                if (!file.getName().contains(".")) {
                    file = new File(file.getAbsolutePath() + ".txt");
                }

                List<Proxy> okList = ProxyManager.getInstance().getProxies().stream()
                        .filter(p -> p.getStatus() == Proxy.Status.OK)
                        .toList();

                try (FileWriter fw = new FileWriter(file)) {
                    for (Proxy p : okList) {
                        fw.write(p.getAddress() + ":" + p.getPort() + "\n");
                    }

                    status.set(tsa("screen.duollectis.proxy_manager.msg.exported", okList.size(), file.getName()), 0xFF88AAFF);
                } catch (IOException e) {
                    status.set(tsa("screen.duollectis.proxy_manager.msg.export_error", e.getMessage()), 0xFFFF4444);
                }
            }
        });
    }

    static void doImport(String text, StatusSink status) {
        Set<Proxy> parsed = AddressParser.parseAll(text);

        if (parsed.isEmpty()) {
            status.set(translatableString("screen.duollectis.proxy_manager.msg.no_valid_proxies"), 0xFFFFAA44);
            return;
        }

        ProxyManager pm = ProxyManager.getInstance();
        pm.addProxies(parsed);
        pm.save();
        status.set(tsa("screen.duollectis.proxy_manager.msg.imported", parsed.size()), 0xFF88AAFF);
    }

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

        ProxyChecker checker = ProxyChecker.getInstance()
                .maxPings(maxPings)
                .maxDelayMs(maxDelay)
                .parallelism(parallelism);

        if (targetText != null && !targetText.isBlank()
                && targetPortText != null && !targetPortText.isBlank()) {
            checker.target(targetText.trim(), parseIntOr(targetPortText, 25565));
        }

        checker.saveSettings();
    }

    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private static void daemonThread(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    interface StatusSink {
        void set(String msg, int color);
    }
}
