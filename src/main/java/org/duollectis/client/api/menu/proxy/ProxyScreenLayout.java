package org.duollectis.client.api.menu.proxy;

import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.ENTRY_H;

/**
 * All layout constants and geometry helper methods for the Proxy Manager screen.
 *
 * <p>Every method is pure (no side effects) and takes the screen {@code width}/{@code height}
 * as parameters so this class has zero dependency on {@link ProxyScreen} itself.
 */
final class ProxyScreenLayout {
    private ProxyScreenLayout() {}

    // ── spacing constants ─────────────────────────────────────────────────────
    static final int PAD     = 6;
    static final int FIELD_H = 20;
    static final int ROW_GAP = 4;
    static final int LABEL_H = 9;
    static final int PORT_W  = 60;
    /** Height consumed by a section header (label + divider line + gap below). */
    static final int SEC_H   = 12;
    /** Height of action buttons under the OK list. */
    static final int OK_BTN_H = FIELD_H;

    /** Width of the small numeric input fields (pings, delay, threads). */
    static final int NUM_W   = 44;

    // ── form section heights (single source of truth) ─────────────────────────

    /**
     * Height of the proxy-form section: 3 label+field rows + type/action row.
     * <pre>
     *   SEC_H                          — section header
     *   (LABEL_H + FIELD_H + ROW_GAP) × 3  — name/addr, port/user, password rows
     *   FIELD_H + ROW_GAP + 8          — type-dropdown / add-save row + extra gap
     * </pre>
     */
    static final int FORM_H =
            SEC_H
            + (LABEL_H + FIELD_H + ROW_GAP) * 3
            + FIELD_H + ROW_GAP + 8;

    /**
     * Height of the checker-settings section.
     * <pre>
     *   SEC_H                          — section header
     *   (FIELD_H + ROW_GAP) × 2        — pings+delay row, threads row
     *   LABEL_H + FIELD_H + ROW_GAP    — target label + target/port/ping-toggle row
     *   FIELD_H + ROW_GAP              — start/stop button row
     * </pre>
     */
    static final int CHECKER_H =
            SEC_H
            + (FIELD_H + ROW_GAP) * 2
            + (LABEL_H + FIELD_H + ROW_GAP)
            + (FIELD_H + ROW_GAP);

    // ── geometry helpers ──────────────────────────────────────────────────────

    static int listsY()           { return PAD + 14; }
    static int listW(int width)   { return (width - PAD * 3) / 2; }
    static int rawListX()         { return PAD; }
    static int okListX(int width) { return PAD * 2 + listW(width); }

    /**
     * Height of the raw (left) list — whatever space remains after the form,
     * progress bar, active-proxy line, and status message.
     */
    static int rawListH(int width, int height) {
        int doneH   = 20 + PAD * 2;
        int activeH = LABEL_H + PAD;
        int barH    = 6 + PAD + LABEL_H + PAD;
        int statusH = LABEL_H + PAD;
        // import/clear/reset button row is already counted inside CHECKER_H
        int importBtnRow = FIELD_H + ROW_GAP;
        return Math.max(40, height - listsY() - FORM_H - CHECKER_H - importBtnRow - doneH - activeH - barH - statusH);
    }

    /** Height of the OK (right) list. */
    static int okListH(int height) {
        int exportBtnH = OK_BTN_H + PAD;
        int doneH      = 20 + PAD * 2;
        return Math.max(40, height - listsY() - exportBtnH - doneH);
    }

    static int controlsY(int width, int height) { return listsY() + rawListH(width, height) + PAD; }
    static int maxRawVis(int width, int height)  { return Math.max(1, rawListH(width, height) / ENTRY_H); }
    static int maxOkVis(int height)              { return Math.max(1, okListH(height) / ENTRY_H); }
}
