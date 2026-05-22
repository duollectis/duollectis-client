package org.duollectis.client.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Общие утилиты проекта. */
public final class Utils {

	private Utils() {}

	/**
	 * Преобразует kebab-case идентификатор модуля в Title Case для отображения в UI.
	 * Например: {@code "auto-log"} → {@code "Auto Log"}.
	 *
	 * @param name идентификатор в формате kebab-case
	 * @return строка в формате Title Case
	 */
	public static String nameToTitle(String name) {
		return Arrays
				.stream(name.split("-"))
				.map(StringUtils::capitalize)
				.collect(Collectors.joining(" "));
	}
}
