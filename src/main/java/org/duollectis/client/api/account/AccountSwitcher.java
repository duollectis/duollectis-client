package org.duollectis.client.api.account;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.session.Session;
import org.duollectis.client.mixin.account.MinecraftClientSessionMixin;

import static org.duollectis.client.utils.MC.mc;

import java.util.Optional;
import java.util.UUID;

/**
 * Утилита мгновенной смены игрового ника без перезапуска клиента.
 * Подменяет объект {@link Session} в {@link MinecraftClient} через Mixin-аксессор.
 * Смена ника работает только локально — сервер видит тот ник, с которым был выполнен вход.
 */
@Slf4j
@UtilityClass
public class AccountSwitcher {

	/**
	 * Применяет аккаунт: создаёт новую {@link Session} с ником и UUID из {@link Account}
	 * и подменяет её в экземпляре {@link MinecraftClient}.
	 *
	 * @param account аккаунт для применения
	 */
	public void apply(Account account) {
		MinecraftClientSessionMixin accessor = (MinecraftClientSessionMixin) mc;

		Session current = accessor.duollectis$getSession();
		UUID uuid = parseUuidOrRandom(account.getUuid());

		Session newSession = new Session(
				account.getUsername(),
				uuid,
				current.getAccessToken(),
				current.getXuid(),
				current.getClientId()
		);

		accessor.duollectis$setSession(newSession);
		AccountManager.getInstance().setActiveAccount(account);

		log.info("Аккаунт переключён на: {}", account.getUsername());
	}

	private static UUID parseUuidOrRandom(String raw) {
		try {
			return UUID.fromString(raw);
		} catch (IllegalArgumentException e) {
			return UUID.randomUUID();
		}
	}
}
