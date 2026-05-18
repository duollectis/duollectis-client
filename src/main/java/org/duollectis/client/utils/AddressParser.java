package org.duollectis.client.utils;

import org.duollectis.client.api.proxy.Proxy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressParser {

    private static final Pattern IP_PORT_COLON = Pattern
            .compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})\\b");

    private static final Pattern IP_PORT_SPACE = Pattern
            .compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\s+(\\d{1,5})\\b");

    private static final Pattern IP_PORT_ANY = Pattern
            .compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\s*[:\\s]+\\s*(\\d{1,5})\\b");

    /**
     * Extracts a single proxy (IP + port) from a line.
     * Returns a Proxy with null address if nothing valid was found.
     */
    public static Proxy extractProxy(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new Proxy(null, -1);
        }

        String trimmedLine = line.trim();

        // Try IP:PORT
        Matcher colonMatcher = IP_PORT_COLON.matcher(trimmedLine);
        if (colonMatcher.find()) {
            String ip = colonMatcher.group(1);
            int port = Integer.parseInt(colonMatcher.group(2));
            if (isValidIp(ip) && isValidPort(port)) {
                return new Proxy(ip, port);
            }
        }

        // Try IP PORT (space/tab)
        Matcher spaceMatcher = IP_PORT_SPACE.matcher(trimmedLine);
        if (spaceMatcher.find()) {
            String ip = spaceMatcher.group(1);
            int port = Integer.parseInt(spaceMatcher.group(2));
            if (isValidIp(ip) && isValidPort(port)) {
                return new Proxy(ip, port);
            }
        }

        // Try any separator
        Matcher anyMatcher = IP_PORT_ANY.matcher(trimmedLine);
        if (anyMatcher.find()) {
            String ip = anyMatcher.group(1);
            int port = Integer.parseInt(anyMatcher.group(2));
            if (isValidIp(ip) && isValidPort(port)) {
                return new Proxy(ip, port);
            }
        }

        return new Proxy(null, -1);
    }

    /**
     * Parses all proxies from a multi-line text block.
     * Each line is tried independently; invalid lines are skipped.
     */
    public static Set<Proxy> parseAll(String text) {
        Set<Proxy> result = new HashSet<>();
        if (text == null || text.isBlank()) return result;

        for (String line : text.split("\\r?\\n")) {
            Proxy p = extractProxy(line);
            if (p.getAddress() != null) {
                result.add(p);
            }
        }

        return result;
    }

    /** Validates an IPv4 address string. */
    public static boolean isValidIp(String ip) {
        if (ip == null) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /** Validates a port number. */
    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }
}
