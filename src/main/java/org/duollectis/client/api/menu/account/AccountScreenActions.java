package org.duollectis.client.api.menu.account;

import lombok.experimental.UtilityClass;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.account.AccountSwitcher;

import static org.duollectis.client.api.menu.account.AccountScreenUtil.translatableString;

/** Логика действий экрана аккаунтов: добавление, удаление и переключение. */
@UtilityClass
final class AccountScreenActions {

	@FunctionalInterface
	interface StatusSink {
		void set(String message, int color);
	}

	static void addAccount(String username, StatusSink status) {
		String trimmed = username.trim();

		if (trimmed.isEmpty()) {
			status.set(translatableString("screen.duollectis.account_manager.msg.empty_name"), 0xFFFF8844);
			return;
		}

		if (trimmed.length() > 16) {
			status.set(translatableString("screen.duollectis.account_manager.msg.name_too_long"), 0xFFFF8844);
			return;
		}

		AccountManager manager = AccountManager.getInstance();

		if (manager.contains(trimmed)) {
			status.set(translatableString("screen.duollectis.account_manager.msg.already_exists"), 0xFFFFAA44);
			return;
		}

		manager.addAccount(new Account(trimmed));
		status.set(translatableString("screen.duollectis.account_manager.msg.added"), 0xFF88FFAA);
	}

	static void removeAccount(Account account, StatusSink status, Runnable onDone) {
		if (account == null) {
			status.set(translatableString("screen.duollectis.account_manager.msg.none_selected"), 0xFFFF8844);
			return;
		}

		AccountManager.getInstance().removeAccount(account);
		status.set(translatableString("screen.duollectis.account_manager.msg.removed"), 0xFFFF8888);
		onDone.run();
	}

	static void switchAccount(Account account, StatusSink status) {
		if (account == null) {
			return;
		}

		AccountSwitcher.apply(account);
		status.set(
				translatableString("screen.duollectis.account_manager.msg.switched") + account.getUsername(),
				0xFF55EE88
		);
	}
}
