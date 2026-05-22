package org.duollectis.client.api.account;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Модель оффлайн-аккаунта: никнейм и UUID для подмены сессии.
 * UUID генерируется детерминированно из никнейма (offline-mode алгоритм),
 * чтобы скин и данные оставались стабильными между перезапусками.
 */
@Getter
public class Account {

	@Setter
	@Expose
	private String username;

	@Setter
	@Expose
	private String uuid;

	@Setter
	@Expose
	private String group = "";

	public String getGroup() {
		return group == null ? "" : group;
	}

	public Account(String username) {
		this.username = username;
		this.uuid = generateOfflineUuid(username).toString();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Account other)) {
			return false;
		}

		return Objects.equals(username, other.username);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(username);
	}

	// UUID в стиле offline-mode: "OfflinePlayer:<ник>"
	private static UUID generateOfflineUuid(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}
}
