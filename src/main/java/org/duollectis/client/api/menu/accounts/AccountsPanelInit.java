package org.duollectis.client.api.menu.accounts;

import org.duollectis.client.api.menu.main.ClientScreen;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;

public final class AccountsPanelInit {

	private AccountsPanelInit() {}

	public static void accountWidgets(ClientScreen screen) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int listY = screen.panelY + PAD;
		int plusBtnW = 20;

		screen.accAddAccountBtn[0] = contentX;
		screen.accAddAccountBtn[1] = listY;
		screen.accAddAccountBtn[2] = plusBtnW;
		screen.accAddAccountBtn[3] = ACC_FIELD_H;

		screen.accAddGroupBtn[0] = contentX + plusBtnW + ACC_ROW_GAP;
		screen.accAddGroupBtn[1] = listY;
		screen.accAddGroupBtn[2] = plusBtnW;
		screen.accAddGroupBtn[3] = ACC_FIELD_H;
	}
}
