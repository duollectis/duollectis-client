package org.duollectis.client.api.menu.account;

/** Константы разметки экрана аккаунтов: отступы, высоты строк и вычисляемые позиции. */
final class AccountScreenLayout {

	static final int PAD = 6;
	static final int FIELD_H = 20;
	static final int ROW_GAP = 4;
	static final int LABEL_H = 9;
	static final int ENTRY_H = 20;
	static final int GROUP_HEADER_H = 18;
	static final int SIDEBAR_W = 180;

	private AccountScreenLayout() {}

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
		return Math.max(40, height - listY() - PAD * 2 - LABEL_H - PAD);
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
