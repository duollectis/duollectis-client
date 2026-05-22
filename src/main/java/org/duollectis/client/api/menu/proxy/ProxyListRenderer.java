package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.proxy.ProxyManager;

import java.util.List;

import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;

/**
 * Утилита отрисовки строк списка прокси: статус, тип, задержка, прогресс-бар
 * и кнопка копирования адреса в буфер обмена.
 */
final class ProxyListRenderer {

 private ProxyListRenderer() {}

 static void renderList(
			DrawContext ctx,
			TextRenderer tr,
			List<ProxyListEntry> entries,
			Proxy editingProxy,
			Proxy activeProxy,
			int lx, int ly, int lw, int lh,
			int scroll, int maxVis,
			int mx, int my
	) {
	ctx.fill(lx, ly, lx + lw, ly + lh, 0xE0080C12);
	drawRect(ctx, lx, ly, lw, lh, 0xFF2A3A5A);

		int visCount = Math.min(maxVis, entries.size() - scroll);

		for (int i = 0; i < visCount; i++) {
			int idx = i + scroll;
			ProxyListEntry entry = entries.get(idx);
			Proxy p = entry.parent();
			int ey = ly + i * ENTRY_H;

			if (ey + ENTRY_H > ly + lh) {
				break;
			}

			boolean isChild = entry.isChild();
			boolean isParent = entry.isDualParent();
			boolean isEditing = !isChild && p == editingProxy;
			boolean isActive = isChild
					? (activeProxy == p && activeProxy.getType() == entry.childType())
					: (p == activeProxy);
			boolean isHovered = mx >= lx && mx < lx + lw && my >= ey && my < ey + ENTRY_H;

	 int bg = isEditing ? 0xBB0E2040
	: isActive ? 0xBB0E2818
	: isChild ? (i % 2 == 0 ? 0x22112233 : 0x18091A2A)
	: isHovered ? 0x33AACCFF
	: (i % 2 == 0 ? 0x18AACCFF : 0x0CAACCFF);
	 ctx.fill(lx + 1, ey, lx + lw - 1, ey + ENTRY_H, bg);

	 if (isEditing) {
	ctx.fill(lx + 1, ey, lx + 3, ey + ENTRY_H, 0xFF5599CC);
	 }

	 if (isActive) {
	ctx.fill(lx + 1, ey, lx + 3, ey + ENTRY_H, 0xFF55EE88);
	 }

			if (!isChild) {
				renderCopyButton(ctx, tr, lx, lw, ey, mx, my);
			}

			int textX = lx + 5 + (isChild ? 14 : 0);

	 if (isParent) {
	ctx.drawTextWithShadow(tr, Text.literal("▶"), lx + 5, ey + 3, 0xFF6A7A8A);
	 }

	 String typeStr = buildTypeString(entry, p);
	 String prefix = isActive ? "► " : "  ";
	 String label = prefix + p.getName() + "  " + p.getAddress() + ":" + p.getPort() + "  " + typeStr;
	 int labelColor = isEditing ? 0xFF77BBEE : isActive ? 0xFF55EE88 : isChild ? 0xFF99CCAA : 0xFFBBCCDD;
	 ctx.drawTextWithShadow(tr, Text.literal(label), textX, ey + 3, labelColor);

			if (!isChild) {
				renderStatusLine(ctx, tr, p, lx, ey);
			}
		}

	if (entries.size() > maxVis) {
	 ctx.drawTextWithShadow(
	tr,
	Text.literal((scroll + 1) + "-" + Math.min(scroll + maxVis, entries.size()) + "/" + entries.size()),
	lx + 2, ly + lh - 10, 0xFF6A7A8A
	 );
	}
	}

	static void renderProgressBar(
			DrawContext ctx,
			TextRenderer tr,
			List<Proxy> all,
			int total, int baseline, int frozenDone,
			int barX, int barY, int barW
	) {
		boolean running = ProxyChecker.getInstance().isRunning();

		int currentDone = (int) all.stream()
				.filter(p -> p.getStatus() == Proxy.Status.OK
						|| p.getStatus() == Proxy.Status.FAILED
						|| p.getStatus() == Proxy.Status.DEAD_IP
						|| p.getStatus() == Proxy.Status.DEAD_SERVER
						|| p.getStatus() == Proxy.Status.PING_ERROR)
				.count();

		int liveDone = Math.max(0, currentDone - baseline);
		int done = Math.clamp(frozenDone >= 0 ? frozenDone : liveDone, 0, total);

		boolean isDone = !running && done >= total;
		float progress = total > 0 ? (float) done / total : 0f;

	ctx.fill(barX, barY, barX + barW, barY + 6, 0xFF0C0F15);

	int fillColor = running ? 0xFF2A5A9A : (isDone ? 0xFF1A6030 : 0xFF3A2A5A);
	int fillW = Math.clamp((int) (barW * progress), 0, barW);
	ctx.fill(barX, barY, barX + fillW, barY + 6, fillColor);
	drawRect(ctx, barX, barY, barW, 6, 0xFF2A3A5A);

	String state;

	if (running) {
	 state = translatableString("screen.duollectis.proxy_manager.progress.checking");
	} else if (isDone) {
	 state = translatableString("screen.duollectis.proxy_manager.progress.done");
	} else {
	 state = translatableString("screen.duollectis.proxy_manager.progress.paused");
	}

	int labelColor = running ? 0xFF77BBEE : (isDone ? 0xFF55EE88 : 0xFFAA88CC);
	ctx.drawTextWithShadow(tr, Text.literal(state + done + " / " + total), barX, barY - 10, labelColor);
	}

	static void renderActiveStatus(DrawContext ctx, TextRenderer tr, int x, int y) {
		Proxy active = ProxyManager.getInstance().getActiveProxy();

		String text = active != null
				? translatableString("screen.duollectis.proxy_manager.active.prefix")
						+ active.getName() + "  [" + active.getType() + "]"
						+ translatableString("screen.duollectis.proxy_manager.entry.ping")
						+ active.getLatency()
						+ translatableString("screen.duollectis.proxy_manager.entry.ping_ms")
				: translatableString("screen.duollectis.proxy_manager.active.none");

	ctx.drawTextWithShadow(tr, Text.literal(text), x, y, active != null ? 0xFF55EE88 : 0xFFEE5555);
	}

	private static String buildTypeString(ProxyListEntry entry, Proxy p) {
		if (entry.isChild()) {
			return "[" + entry.childType() + "]";
		}

		if (entry.isDualParent()) {
			StringBuilder sb = new StringBuilder("[");
			boolean first = true;

			for (Proxy.Type t : p.getSupportedTypes()) {
				if (first) {
					first = false;
				}
				else {
					sb.append("+");
				}

				sb.append(t.name());
			}

			sb.append("]");
			return sb.toString();
		}

		return p.getType() != null ? "[" + p.getType() + "]" : "[?]";
	}

	private static void renderCopyButton(DrawContext ctx, TextRenderer tr, int lx, int lw, int ey, int mx, int my) {
		String copyLabel = translatableString("screen.duollectis.proxy_manager.entry.copy_btn");
		int copyW = tr.getWidth(copyLabel) + 4;
		int copyX = lx + lw - copyW - 2;
		int copyY = ey + (ENTRY_H - 9) / 2;
		boolean copyHovered = mx >= copyX && mx < copyX + copyW && my >= ey && my < ey + ENTRY_H;

	ctx.fill(copyX, ey + 2, copyX + copyW, ey + ENTRY_H - 2, copyHovered ? 0x661E2E48 : 0x44141C2C);
	ctx.drawTextWithShadow(
	 tr,
	 Text.literal(copyLabel),
	 copyX + 2, copyY,
	 copyHovered ? 0xFFCDD5E0 : 0xFF6A7A8A
	);
	}

	private static void renderStatusLine(DrawContext ctx, TextRenderer tr, Proxy p, int lx, int ey) {
		String statusStr = switch (p.getStatus()) {
			case UNKNOWN -> translatableString("screen.duollectis.proxy_manager.status.unknown");
			case CHECKING -> translatableString("screen.duollectis.proxy_manager.status.checking");
			case FAILED -> translatableString("screen.duollectis.proxy_manager.status.failed");
			case DEAD_IP -> translatableString("screen.duollectis.proxy_manager.status.dead_ip");
			case DEAD_SERVER -> translatableString("screen.duollectis.proxy_manager.status.dead_server");
			case PING_ERROR -> translatableString("screen.duollectis.proxy_manager.status.ping_error");
			case OK -> translatableString("screen.duollectis.proxy_manager.status.ok");
		};

		String pingStr = p.getLatency() >= 0
				? translatableString("screen.duollectis.proxy_manager.entry.ping")
						+ p.getLatency()
						+ translatableString("screen.duollectis.proxy_manager.entry.ping_ms")
				: "";

		String pingCountStr = p.getStatus() == Proxy.Status.OK && p.getPingCount() > 0
				? " ×" + p.getPingCount()
				: "";

		ctx.drawTextWithShadow(
				tr,
				Text.literal("  " + statusStr + pingStr + pingCountStr),
				lx + 5, ey + 13, p.getStatus().color
		);
	}
}
