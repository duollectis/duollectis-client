package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.proxy.ProxyManager;

import java.util.List;

import static org.duollectis.client.api.menu.proxy.ProxyScreenKeys.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.*;

/**
 * Per-frame rendering helpers for {@link ProxyScreen}.
 *
 * <p>All methods are static and receive the screen as the first parameter so
 * they can read its package-private fields without subclassing.
 */
final class ProxyScreenRenderer {
    private ProxyScreenRenderer() {}

    /**
     * Renders both proxy list columns with mouse coordinates for hover effects.
     */
    static void renderLists(ProxyScreen s, DrawContext ctx,
                            List<Proxy> rawList, List<Proxy> okList,
                            int mx, int my) {
        int lw = s.lw(), ly = s.ly(), rlh = s.rlh(), olh = s.olh();

        s.rawScroll = Math.min(s.rawScroll, Math.max(0, rawList.size() - s.mrv()));
        ProxyListRenderer.renderRawList(ctx, s.tr(), rawList, s.editingProxy,
                s.lx(), ly, lw, rlh, s.rawScroll, s.mrv(), mx, my);

        Proxy active = ProxyManager.getInstance().getActiveProxy();
        s.okScroll = Math.min(s.okScroll, Math.max(0, okList.size() - s.mov()));
        ProxyListRenderer.renderOkList(ctx, s.tr(), okList, active,
                s.ox(), ly, lw, olh, s.okScroll, s.mov(), mx, my);
    }

    /** Renders the form section headers and field labels below the lists. */
    static void renderFormLabels(ProxyScreen s, DrawContext ctx) {
        int lx = s.lx(), lw = s.lw(), py = s.cy();
        int halfLw = (lw - ROW_GAP) / 2;

        drawSectionHeader(ctx, s.tr(), lx, py, lw,
                s.editingProxy != null ? ts(SEC_EDIT) : ts(SEC_ADD));
        py += SEC_H;
        ctx.drawTextWithShadow(s.tr(), t(LBL_NAME),     lx,                    py, 0xFFAAAAAA);
        ctx.drawTextWithShadow(s.tr(), t(LBL_ADDRESS),  lx + halfLw + ROW_GAP, py, 0xFFAAAAAA);
        py += LABEL_H + FIELD_H + ROW_GAP;
        ctx.drawTextWithShadow(s.tr(), t(LBL_PORT),     lx,                    py, 0xFFAAAAAA);
        ctx.drawTextWithShadow(s.tr(), t(LBL_USERNAME), lx + halfLw + ROW_GAP, py, 0xFFAAAAAA);
        py += LABEL_H + FIELD_H + ROW_GAP;
        ctx.drawTextWithShadow(s.tr(), t(LBL_PASSWORD), lx, py, 0xFFAAAAAA);
        py += LABEL_H + FIELD_H + ROW_GAP + FIELD_H + ROW_GAP + 8;

        drawSectionHeader(ctx, s.tr(), lx, py, lw, ts(SEC_CHECKER));
        py += SEC_H;

        int labelW = s.tr().getWidth(t(LBL_MAX_PINGS)) + 4;
        int numW   = 44;
        ctx.drawTextWithShadow(s.tr(), t(LBL_MAX_PINGS), lx, py + 6, 0xFFAAAAAA);
        ctx.drawTextWithShadow(s.tr(), t(LBL_DELAY_MS),
                lx + labelW + numW + ROW_GAP, py + 6, 0xFFAAAAAA);
        py += FIELD_H + ROW_GAP;
        ctx.drawTextWithShadow(s.tr(), t(LBL_THREADS), lx, py + 6, 0xFFAAAAAA);
        py += FIELD_H + ROW_GAP;
        ctx.drawTextWithShadow(s.tr(), t(LBL_TARGET), lx, py, 0xFFAAAAAA);
    }

    /** Renders the edit-mode indicator, progress bar, and status message. */
    static void renderOverlay(ProxyScreen s, DrawContext ctx, List<Proxy> all) {
        // edit mode indicator
        if (s.editingProxy != null) {
            ctx.drawTextWithShadow(s.tr(),
                    Text.literal("✎ " + s.editingProxy.getName()),
                    s.lx(), s.cy() - 1, 0xFF88CCFF);
        }

        // active proxy line
        ProxyListRenderer.renderActiveStatus(ctx, s.tr(), PAD, s.height - PAD - 12);

        // progress bar
        if (ProxyScreen.barSessionTotal >= 0) {
            ProxyListRenderer.renderProgressBar(ctx, s.tr(), all,
                    ProxyScreen.barSessionTotal,
                    ProxyScreen.barSessionBaseline,
                    ProxyScreen.barSessionDone,
                    s.lx(), s.height - PAD - 32, s.lw());
        }

        // status message
        if (!s.statusMsg.isEmpty()) {
            ctx.drawTextWithShadow(s.tr(), Text.literal(s.statusMsg),
                    PAD, s.height - PAD - 56, s.statusColor);
        }
    }

    /** Updates per-frame button active states and dynamic labels. */
    static void updateButtonStates(ProxyScreen s, List<Proxy> all, List<Proxy> okList) {
        boolean running          = ProxyChecker.getInstance().isRunning();
        boolean hasFailed        = all.stream().anyMatch(Proxy::isFailed);
        boolean hasRaw           = all.stream().anyMatch(p -> p.getStatus() == Proxy.Status.UNKNOWN || p.isFailed());
        boolean hasNonUnknownRaw = all.stream().anyMatch(p -> p.isFailed() || p.getStatus() == Proxy.Status.CHECKING);
        boolean hasOk            = !okList.isEmpty();
        boolean hasUnknown       = all.stream().anyMatch(p -> p.getStatus() == Proxy.Status.UNKNOWN);

        if (s.btnStartStop  != null) {
            s.btnStartStop.setMessage(t(running ? BTN_STOP : BTN_START));
            s.btnStartStop.active = running || hasUnknown;
        }
        if (s.btnDelFailed  != null) s.btnDelFailed.active  = hasFailed && !running;
        if (s.btnClear      != null) s.btnClear.active      = hasRaw && !running;
        if (s.btnResetRaw   != null) s.btnResetRaw.active   = hasNonUnknownRaw && !running;
        if (s.btnExportOk   != null) s.btnExportOk.active   = hasOk;
        if (s.btnResetChk   != null) s.btnResetChk.active   = hasOk && !running;
        if (s.btnRecheck    != null) s.btnRecheck.active    = hasOk && !running;
        if (s.btnSort       != null) {
            s.btnSort.setMessage(ProxyOkSorter.sortLabel(s.okSortMode));
            s.btnSort.active = hasOk;
        }
        if (s.btnPingToggle != null) {
            boolean s3 = ProxyChecker.getInstance().getSettings().isPingStage3();
            s.btnPingToggle.setMessage(t(s3 ? BTN_PING_ON : BTN_PING_OFF));
            s.btnPingToggle.active = !running;
        }
    }
}
