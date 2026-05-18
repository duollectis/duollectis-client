package org.duollectis.client.api.menu.proxy;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Proxy manager UI built with Cloth Config.
 *
 * Each proxy is stored as a single string in the format:
 *   name|address:port|SOCKS4|username|password
 * Fields after port are optional.
 *
 * The "Active proxy" field accepts the proxy name (or address:port if no name was given).
 * Leave it blank to disable the proxy.
 */
public class ProxyMenu {

    public static void open() {
        MinecraftClient client = MinecraftClient.getInstance();
        ProxyManager pm = ProxyManager.getInstance();

        // ── Serialize current proxy list to strings ───────────────────────────
        List<String> proxyStrings = new ArrayList<>();
        for (Proxy p : pm.getProxies()) {
            proxyStrings.add(serialize(p));
        }

        String activeProxyName = pm.getActiveProxy() != null ? pm.getActiveProxy().getName() : "";

        // ── Build Cloth Config screen ─────────────────────────────────────────
        ConfigBuilder builder = ConfigBuilder
                .create()
                .setParentScreen(client.currentScreen)
                .setTitle(Text.literal("Proxy Manager"))
                .setSavingRunnable(() -> {});   // we handle saving ourselves

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Proxies"));

        // ── Proxy list (one entry per line) ───────────────────────────────────
        category.addEntry(eb
                .startStrList(
                        Text.literal("Proxy List"),
                        proxyStrings
                )
                .setDefaultValue(new ArrayList<>())
                .setTooltip(
                        Text.literal("One proxy per line."),
                        Text.literal("Format: name|host:port|SOCKS5|user|pass"),
                        Text.literal("  name     – any label (optional, use - to skip)"),
                        Text.literal("  host:port – e.g. 1.2.3.4:1080"),
                        Text.literal("  type     – SOCKS4 or SOCKS5 (default SOCKS5)"),
                        Text.literal("  user     – username (optional, use - to skip)"),
                        Text.literal("  pass     – password (optional)")
                )
                .setSaveConsumer(list -> {
                    pm.getProxies().clear();
                    for (String s : list) {
                        Proxy p = deserialize(s);
                        if (p != null) pm.getProxies().add(p);
                    }
                    pm.save();
                })
                .build()
        );

        // ── Active proxy selector ─────────────────────────────────────────────
        category.addEntry(eb
                .startStrField(
                        Text.literal("Active Proxy (name or host:port)"),
                        activeProxyName
                )
                .setDefaultValue("")
                .setTooltip(
                        Text.literal("Enter the name (or host:port) of the proxy to use."),
                        Text.literal("Leave blank to connect without a proxy.")
                )
                .setSaveConsumer(name -> {
                    if (name == null || name.isBlank()) {
                        pm.setActiveProxy(null);
                        return;
                    }
                    for (Proxy p : pm.getProxies()) {
                        if (p.getName().equalsIgnoreCase(name.trim())) {
                            pm.setActiveProxy(p);
                            return;
                        }
                    }
                    // not found – clear active
                    pm.setActiveProxy(null);
                })
                .build()
        );

        client.setScreen(builder.build());
    }

    // ── Serialization helpers ─────────────────────────────────────────────────

    /**
     * Converts a Proxy to a human-readable/editable string:
     *   name|host:port|TYPE|user|pass
     * Optional fields are represented as "-" when absent.
     */
    private static String serialize(Proxy p) {
        String name     = p.getName()     != null && !p.getName().isBlank()     ? p.getName()     : "-";
        String user     = p.getUsername() != null && !p.getUsername().isBlank() ? p.getUsername() : "-";
        String pass     = p.getPassword() != null && !p.getPassword().isBlank() ? p.getPassword() : "-";
        return name + "|" + p.getAddress() + ":" + p.getPort() + "|" + p.getType().name() + "|" + user + "|" + pass;
    }

    /**
     * Parses a proxy string back into a Proxy object.
     * Returns null if the string is malformed.
     *
     * Accepted formats (fields separated by |):
     *   host:port
     *   host:port|TYPE
     *   name|host:port
     *   name|host:port|TYPE
     *   name|host:port|TYPE|user
     *   name|host:port|TYPE|user|pass
     */
    static Proxy deserialize(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();

        String[] parts = raw.split("\\|", -1);

        String name, hostPort, typePart, user, pass;

        // Detect whether the first token is a name or host:port
        if (parts[0].contains(":")) {
            // No name given – first token is host:port
            name     = null;
            hostPort = parts[0];
            typePart = parts.length > 1 ? parts[1] : "SOCKS5";
            user     = parts.length > 2 ? parts[2] : null;
            pass     = parts.length > 3 ? parts[3] : null;
        } else {
            name     = parts[0].equals("-") ? null : parts[0];
            hostPort = parts.length > 1 ? parts[1] : null;
            typePart = parts.length > 2 ? parts[2] : "SOCKS5";
            user     = parts.length > 3 ? parts[3] : null;
            pass     = parts.length > 4 ? parts[4] : null;
        }

        if (hostPort == null || !hostPort.contains(":")) return null;

        int lastColon = hostPort.lastIndexOf(':');
        String host = hostPort.substring(0, lastColon);
        int port;
        try {
            port = Integer.parseInt(hostPort.substring(lastColon + 1));
        } catch (NumberFormatException e) {
            return null;
        }

        Proxy.Type type;
        try {
            type = Proxy.Type.valueOf(typePart.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = Proxy.Type.SOCKS5;
        }

        if (user != null && user.equals("-")) user = null;
        if (pass != null && pass.equals("-")) pass = null;

        String displayName = (name != null && !name.isBlank()) ? name : host + ":" + port;

        return new Proxy(displayName, host, port, type, user, pass);
    }
}
