package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.proxy.ProxyManager;

import java.util.List;

import static org.duollectis.client.api.menu.proxy.ProxyScreenKeys.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;

/**
 * Stateless rendering helpers for the two proxy lists and the progress bar.
 * All methods are package-private and called from {@link ProxyScreen#render}.
 */
final class ProxyListRenderer {
    private ProxyListRenderer() {}

    // ── raw list (left) ───────────────────────────────────────────────────────

    static void renderRawList(DrawContext ctx, TextRenderer tr,
                              List<Proxy> rawList, Proxy editingProxy,
                              int rx, int ly, int lw, int rlh,
                              int rawScroll, int maxRawV,
                              int mx, int my) {
        ctx.drawTextWithShadow(tr,
                Text.literal(ts(LIST_RAW) + " (" + rawList.size() + ")"),
                rx, ly - 10, 0xFFAAAAAA);
        ctx.fill(rx, ly, rx + lw, ly + rlh, 0x88000000);
        drawRect(ctx, rx, ly, lw, rlh, 0xFF444444);

        int visCount = Math.min(maxRawV, rawList.size() - rawScroll);
        for (int i = 0; i < visCount; i++) {
            int idx = i + rawScroll;
            Proxy p  = rawList.get(idx);
            int ey   = ly + i * ENTRY_H;
            if (ey + ENTRY_H > ly + rlh) break;

            boolean isEditing = p == editingProxy;
            boolean isHovered = mx >= rx && mx < rx + lw && my >= ey && my < ey + ENTRY_H;
            int bg = isEditing  ? 0xAA004466
                    : isHovered ? 0x55FFFFFF
                    : (i % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF);
            ctx.fill(rx + 1, ey, rx + lw - 1, ey + ENTRY_H, bg);
            if (isEditing) ctx.fill(rx + 1, ey, rx + 3, ey + ENTRY_H, 0xFF44AAFF);

            String typeStr = p.getType() != null ? "[" + p.getType() + "]" : "[?]";
            String label   = p.getName() + "  " + p.getAddress() + ":" + p.getPort() + "  " + typeStr;
            ctx.drawTextWithShadow(tr, Text.literal(label),
                    rx + 5, ey + 3, isEditing ? 0xFF88CCFF : 0xFFCCCCCC);

            String statusStr = switch (p.getStatus()) {
                case UNKNOWN     -> ts(ST_UNKNOWN);
                case CHECKING    -> ts(ST_CHECKING);
                case FAILED      -> ts(ST_FAILED);
                case DEAD_IP     -> ts(ST_DEAD_IP);
                case DEAD_SERVER -> ts(ST_DEAD_SERVER);
                case PING_ERROR  -> ts(ST_PING_ERROR);
                default          -> p.getStatus().name();
            };
            String pingStr = p.getLatency() >= 0 ? ts(ENTRY_PING) + p.getLatency() + ts(ENTRY_PING_MS) : "";
            ctx.drawTextWithShadow(tr,
                    Text.literal("  " + statusStr + pingStr),
                    rx + 5, ey + 13, p.getStatus().color);
        }

        if (rawList.size() > maxRawV) {
            ctx.drawTextWithShadow(tr,
                    Text.literal((rawScroll + 1) + "-"
                            + Math.min(rawScroll + maxRawV, rawList.size()) + "/" + rawList.size()),
                    rx + 2, ly + rlh - 10, 0xFF888888);
        }
    }

    // ── OK list (right) ───────────────────────────────────────────────────────

    static void renderOkList(DrawContext ctx, TextRenderer tr,
                             List<Proxy> okList, Proxy activeProxy,
                             int ox, int ly, int lw, int olh,
                             int okScroll, int maxOkV,
                             int mx, int my) {
        ctx.drawTextWithShadow(tr,
                Text.literal(ts(LIST_CHECKED) + " (" + okList.size() + ")"),
                ox, ly - 10, 0xFF88FF88);
        ctx.fill(ox, ly, ox + lw, ly + olh, 0x88000000);
        drawRect(ctx, ox, ly, lw, olh, 0xFF446644);

        int visCount = Math.min(maxOkV, okList.size() - okScroll);
        for (int i = 0; i < visCount; i++) {
            int idx = i + okScroll;
            Proxy p  = okList.get(idx);
            int ey   = ly + i * ENTRY_H;
            if (ey + ENTRY_H > ly + olh) break;

            boolean isActive  = p == activeProxy;
            boolean isHovered = mx >= ox && mx < ox + lw && my >= ey && my < ey + ENTRY_H;
            int bg = isActive  ? 0xAA006600
                    : isHovered ? 0x55FFFFFF
                    : (i % 2 == 0 ? 0x22FFFFFF : 0x11FFFFFF);
            ctx.fill(ox + 1, ey, ox + lw - 1, ey + ENTRY_H, bg);
            if (isActive) ctx.fill(ox + 1, ey, ox + 3, ey + ENTRY_H, 0xFF00FF00);

            // copy button
            String copyLabel = ts(ENTRY_COPY_BTN);
            int copyW = tr.getWidth(copyLabel) + 4;
            int copyX = ox + lw - copyW - 2;
            int copyY = ey + (ENTRY_H - 9) / 2;
            boolean copyHovered = mx >= copyX && mx < copyX + copyW && my >= ey && my < ey + ENTRY_H;
            ctx.fill(copyX, ey + 2, copyX + copyW, ey + ENTRY_H - 2,
                    copyHovered ? 0x88AAAAAA : 0x44888888);
            ctx.drawTextWithShadow(tr, Text.literal(copyLabel),
                    copyX + 2, copyY, copyHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

            String typeStr = p.getType() != null ? "[" + p.getType() + "]" : "[?]";
            String prefix  = isActive ? "► " : "  ";
            String label   = prefix + p.getName() + "  " + p.getAddress() + ":" + p.getPort() + "  " + typeStr;
            ctx.drawTextWithShadow(tr, Text.literal(label),
                    ox + 5, ey + 3, isActive ? 0xFF88FF88 : 0xFFCCCCCC);

            String pingStr = p.getLatency() >= 0 ? p.getLatency() + ts(ENTRY_PING_MS) : "—";
            String cntStr  = p.getPingCount() > 0 ? " ×" + p.getPingCount() : "";
            ctx.drawTextWithShadow(tr,
                    Text.literal("  " + ts(ST_OK) + ts(ENTRY_PING) + pingStr + cntStr),
                    ox + 5, ey + 13, Proxy.Status.OK.color);
        }

        if (okList.size() > maxOkV) {
            ctx.drawTextWithShadow(tr,
                    Text.literal((okScroll + 1) + "-"
                            + Math.min(okScroll + maxOkV, okList.size()) + "/" + okList.size()),
                    ox + 2, ly + olh - 10, 0xFF888888);
        }
    }

    // ── progress bar ──────────────────────────────────────────────────────────

    static void renderProgressBar(DrawContext ctx, TextRenderer tr,
                                  List<Proxy> all,
                                  int barSessionTotal, int barSessionBaseline, int barSessionDone,
                                  int barX, int barY, int barW) {
        boolean running = ProxyChecker.getInstance().isRunning();

        int currentDone = (int) all.stream()
                .filter(p -> p.getStatus() == Proxy.Status.OK
                          || p.getStatus() == Proxy.Status.FAILED
                          || p.getStatus() == Proxy.Status.DEAD_IP
                          || p.getStatus() == Proxy.Status.DEAD_SERVER
                          || p.getStatus() == Proxy.Status.PING_ERROR)
                .count();
        int liveDone = Math.max(0, currentDone - barSessionBaseline);
        int barDone  = barSessionDone >= 0 ? barSessionDone : liveDone;
        barDone = Math.max(0, Math.min(barDone, barSessionTotal));

        boolean isDone    = !running && barDone >= barSessionTotal;
        float   progress  = barSessionTotal > 0 ? (float) barDone / barSessionTotal : 0f;

        // background
        ctx.fill(barX, barY, barX + barW, barY + 6, 0xFF222222);
        // fill — blue=running, purple=paused, green=done
        int fillColor = running ? 0xFF4488FF : (isDone ? 0xFF33AA33 : 0xFF884488);
        int fillW = Math.max(0, Math.min(barW, (int)(barW * progress)));
        ctx.fill(barX, barY, barX + fillW, barY + 6, fillColor);
        // border
        drawRect(ctx, barX, barY, barW, 6, 0xFF555555);
        // label
        String state = running ? ts(PROG_CHECKING) : (isDone ? ts(PROG_DONE) : ts(PROG_PAUSED));
        String prog  = state + barDone + " / " + barSessionTotal;
        int labelColor = running ? 0xFF88AAFF : (isDone ? 0xFF88FF88 : 0xFFCC88FF);
        ctx.drawTextWithShadow(tr, Text.literal(prog), barX, barY - 10, labelColor);
    }

    // ── active proxy status line ──────────────────────────────────────────────

    static void renderActiveStatus(DrawContext ctx, TextRenderer tr, int x, int y) {
        Proxy active = ProxyManager.getInstance().getActiveProxy();
        String text = active != null
                ? ts(ACTIVE_PREFIX) + active.getName() + "  [" + active.getType() + "]"
                  + ts(ENTRY_PING) + active.getLatency() + ts(ENTRY_PING_MS)
                : ts(ACTIVE_NONE);
        ctx.drawTextWithShadow(tr, Text.literal(text), x, y,
                active != null ? 0xFF88FF88 : 0xFFFF8888);
    }
}
