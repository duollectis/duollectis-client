package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.proxy.ProxyManager;

import java.util.List;

import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;

/**
 * Утилита рендеринга экрана прокси: отрисовывает список прокси, боковую панель
 * с метками, прогресс-бар чекера и управляет состоянием кнопок.
 */
final class ProxyScreenRenderer {

 private ProxyScreenRenderer() {}

 static void renderList(ProxyScreen s, DrawContext ctx, List<ProxyListEntry> entries, int mx, int my) {
		int lx = s.lx(), ly = s.ly(), lw = s.lw(), lh = s.lh();

		s.listScroll = Math.clamp(entries.size() - s.mv(), 0, s.listScroll);

		ctx.drawTextWithShadow(
			s.textRenderer(),
			Text.literal(
				translatableString("screen.duollectis.proxy_manager.list.raw")
					+ " (" + s.cachedList.size() + ")"
					+ "  [" + s.filterDropdown.getSelected().name() + "]"
			),
			lx, ly - 10, 0xFF6A7A8A
		);

		ProxyListRenderer.renderList(
				ctx,
				s.textRenderer(),
				entries,
				s.editingProxy,
				ProxyManager.getInstance().getActiveProxy(),
				lx, ly, lw, lh,
				s.listScroll, s.mv(),
				mx, my
		);
	}

	static void renderSidebarLabels(ProxyScreen s, DrawContext ctx) {
		int sx = s.sx(), sw = s.sw(), py = sidebarY();
		int portW = (sw - ROW_GAP) / 2;
		int addrW = sw - portW - ROW_GAP;

		drawSectionHeader(
				ctx, s.textRenderer(), sx, py, sw,
				s.editingProxy == null
				? translatableString("screen.duollectis.proxy_manager.section.add_proxy")
				: translatableString("screen.duollectis.proxy_manager.section.edit_proxy")
		);
		py += SEC_H;

		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.name"),
			sx,
			py,
			0xFF6A7A8A
		);
		py += LABEL_H + FIELD_H + ROW_GAP;

		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.address"),
			sx,
			py,
			0xFF6A7A8A
		);
		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.port"),
			sx + addrW + ROW_GAP,
			py,
			0xFF6A7A8A
		);
		py += LABEL_H + FIELD_H + ROW_GAP;

		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.username"),
			sx,
			py,
			0xFF6A7A8A
		);
		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.password"),
			sx + addrW + ROW_GAP,
			py,
			0xFF6A7A8A
		);

		py = sidebarY() + FORM_H;
		drawSectionHeader(
				ctx,
				s.textRenderer(),
				sx,
				py,
				sw,
				translatableString("screen.duollectis.proxy_manager.section.checker")
		);
		py += SEC_H;

		int maxPingsLabelW = s
				.textRenderer()
				.getWidth(translatable("screen.duollectis.proxy_manager.label.max_pings")) + 4;

		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.max_pings"),
			sx,
			py + 6,
			0xFF6A7A8A
		);

		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.delay_ms"),
			sx + maxPingsLabelW + NUM_W + ROW_GAP,
			py + 6,
			0xFF6A7A8A
		);

		py += FIELD_H + ROW_GAP;

		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.threads"),
			sx,
			py + 6,
			0xFF6A7A8A
		);

		py += FIELD_H + ROW_GAP;

		ctx.drawTextWithShadow(
			s.textRenderer(),
			translatable("screen.duollectis.proxy_manager.label.target"),
			sx,
			py,
			0xFF6A7A8A
		);

		ctx.fill(sx - PAD / 2, sidebarY() - 2, sx - PAD / 2 + 1, s.height - PAD, 0xFF1E2A40);
	}

	static void renderOverlay(ProxyScreen s, DrawContext ctx, List<Proxy> all) {
		int lx = s.lx(), lw = s.lw();

		if (s.editingProxy != null) {
			ctx.drawTextWithShadow(
				s.textRenderer(),
				Text.literal("✎ " + s.editingProxy.getName()),
				lx, s.ly() - 1, 0xFF77BBEE
			);
		}

		int bottomY = s.ly() + s.lh() + PAD;

		if (!s.statusMsg.isEmpty()) {
			ctx.drawTextWithShadow(
					s.textRenderer(),
					Text.literal(s.statusMsg),
					lx, bottomY, s.statusColor
			);
		}

		bottomY += LABEL_H + PAD;

		if (ProxyScreen.barTotal >= 0) {
			ProxyListRenderer.renderProgressBar(
					ctx, s.textRenderer(), all,
					ProxyScreen.barTotal, ProxyScreen.barBaseline, ProxyScreen.barDone,
					lx, bottomY, lw
			);
		}

		bottomY += 6 + PAD + LABEL_H + PAD;

		ProxyListRenderer.renderActiveStatus(ctx, s.textRenderer(), lx, bottomY);
	}

	static void updateButtonStates(ProxyScreen s, List<Proxy> all) {
		boolean running = ProxyChecker.getInstance().isRunning();
		boolean hasFailed = all.stream().anyMatch(Proxy::isFailed);
		boolean hasRaw = all.stream().anyMatch(p -> p.getStatus() == Proxy.Status.UNKNOWN || p.isFailed());
		boolean hasNonUnknownRaw = all.stream().anyMatch(p -> p.isFailed() || p.getStatus() == Proxy.Status.CHECKING);
		boolean hasOk = all.stream().anyMatch(p -> p.getStatus() == Proxy.Status.OK);
		boolean hasUnknown = all.stream().anyMatch(p -> p.getStatus() == Proxy.Status.UNKNOWN);

		if (s.btnStartStop != null) {
			s.btnStartStop.setMessage(translatable(running
			                                       ? "screen.duollectis.proxy_manager.btn.stop"
			                                       : "screen.duollectis.proxy_manager.btn.start"));
			s.btnStartStop.active = running || hasUnknown;
		}

		if (s.btnDelFailed != null) {
			s.btnDelFailed.active = hasFailed && !running;
		}

		if (s.btnClear != null) {
			s.btnClear.active = hasRaw && !running;
		}

		if (s.btnResetRaw != null) {
			s.btnResetRaw.active = hasNonUnknownRaw && !running;
		}

		if (s.btnExportOk != null) {
			s.btnExportOk.active = hasOk;
		}

		if (s.btnResetChk != null) {
			s.btnResetChk.active = hasOk && !running;
		}

		if (s.btnPingToggle != null) {
			boolean pingEnabled = ProxyChecker.getInstance().getSettings().isPingStage3();
			s.btnPingToggle.setMessage(translatable(pingEnabled
			                                        ? "screen.duollectis.proxy_manager.btn.ping_on"
			                                        : "screen.duollectis.proxy_manager.btn.ping_off"));
			s.btnPingToggle.active = !running;
		}
	}
}
