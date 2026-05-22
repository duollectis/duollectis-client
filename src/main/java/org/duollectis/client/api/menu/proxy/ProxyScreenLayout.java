package org.duollectis.client.api.menu.proxy;

import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.ENTRY_H;

/** Константы разметки экрана прокси: отступы, высоты секций и вычисляемые позиции. */
final class ProxyScreenLayout {

	static final int PAD = 6;
	static final int FIELD_H = 20;
	static final int ROW_GAP = 4;
	static final int LABEL_H = 9;
	static final int PORT_W = 60;
	static final int SEC_H = 12;
	static final int NUM_W = 44;
	static final int SIDEBAR_W = 200;

	static final int FORM_H =
		SEC_H
		+ (LABEL_H + FIELD_H + ROW_GAP) * 3
		+ FIELD_H + ROW_GAP + 8;

	static final int CHECKER_H =
		SEC_H
		+ (FIELD_H + ROW_GAP) * 2
		+ (LABEL_H + FIELD_H + ROW_GAP);

	private ProxyScreenLayout() {}

	static int listX() {
		return PAD;
	}

	static int listY() {
		return PAD + 14;
	}

	static int listW(int width) {
		return width - SIDEBAR_W - PAD * 3;
	}

	static int listH(int height) {
		int bottomH = (LABEL_H + PAD)
			+ (6 + PAD + LABEL_H + PAD)
			+ (LABEL_H + PAD);
		return Math.max(40, height - listY() - bottomH - PAD);
	}

	static int maxVis(int height) {
		return Math.max(1, listH(height) / ENTRY_H);
	}

	static int sidebarX(int width) {
		return width - SIDEBAR_W - PAD;
	}

	static int sidebarY() {
		return PAD + 14;
	}

	static int sidebarW() {
		return SIDEBAR_W;
	}
}
