package org.duollectis.client.api.menu.account;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static org.duollectis.client.api.menu.account.AccountScreenLayout.*;
import static org.duollectis.client.api.menu.account.AccountScreenUtil.*;

/** Инициализатор виджетов экрана аккаунтов: поле ввода ника и кнопки управления. */
final class AccountScreenInit {

	private AccountScreenInit() {}

	static void widgets(AccountScreen s) {
		int sx = sidebarX(s.width);
		int sw = sidebarW();
		int py = sidebarY() + LABEL_H + 4;

		s.nicknameField = s.register(field(
				s.textRenderer(),
				sx,
				py,
				sw,
				"screen.duollectis.account_manager.field.nickname"
		));
		s.nicknameField.setMaxLength(16);
		py += FIELD_H + ROW_GAP;

		int halfW = (sw - ROW_GAP) / 2;

		s.register(btn(
				"screen.duollectis.account_manager.btn.add",
				sx,
				py,
				halfW,
				b -> s.addAccount()
		));

		s.register(btn(
				"screen.duollectis.account_manager.btn.remove",
				sx + halfW + ROW_GAP,
				py,
				halfW,
				b -> s.removeSelected()
		));
		py += FIELD_H + ROW_GAP * 2;

		s.register(btn(
				"screen.duollectis.account_manager.btn.switch",
				sx,
				py,
				sw,
				b -> s.switchSelected()
		));
	}

	private static ButtonWidget btn(
			String key,
			int x,
			int y,
			int w,
			ButtonWidget.PressAction action
	) {
		return ButtonWidget.builder(Text.translatable(key), action)
				.dimensions(x, y, w, FIELD_H)
				.build();
	}
}
