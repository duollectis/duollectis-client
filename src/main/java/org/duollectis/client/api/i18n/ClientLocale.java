package org.duollectis.client.api.i18n;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.duollectis.client.utils.INamed;

@Getter
@RequiredArgsConstructor
public enum ClientLocale implements INamed {

	ENGLISH("en_us", "English"),
	RUSSIAN("ru_ru", "Русский");

	private final String fileName;
	private final String name;

	@Override
	public String getName() {
		return name;
	}
}
