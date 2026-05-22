package org.duollectis.client.utils;

import org.duollectis.client.api.proxy.Proxy;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Утилита для извлечения прокси-адресов из произвольного текста.
 * Поддерживает форматы {@code ip:port} и {@code ip port} с валидацией октетов и диапазона порта.
 */
public final class AddressParser {

	private static final int MIN_PORT = 1;
	private static final int MAX_PORT = 65535;
	private static final int IP_OCTET_COUNT = 4;
	private static final int MAX_OCTET_VALUE = 255;

	private static final Pattern IP_PORT_PATTERN = Pattern.compile(
			"\\b(?<ip>\\d{1,3}(?:\\.\\d{1,3}){3})\\s*[:\\s]\\s*(?<port>\\d{1,5})\\b"
	);

	/**
	 * Извлекает первый валидный прокси из одной строки текста.
	 *
	 * @param line строка, потенциально содержащая {@code ip:port}
	 * @return {@link Optional} с прокси, или пустой если строка не содержит валидного адреса
	 */
	private AddressParser() {}

	public static Optional<Proxy> extractProxy(String line) {
		if (line == null || line.isBlank()) {
			return Optional.empty();
		}

		Matcher matcher = IP_PORT_PATTERN.matcher(line.trim());

		if (!matcher.find()) {
			return Optional.empty();
		}

		String ip = matcher.group(1);
		int port = Integer.parseInt(matcher.group(2));

		return isValidIp(ip) && isValidPort(port)
		       ? Optional.of(new Proxy(ip, port))
		       : Optional.empty();
	}

	/**
	 * Парсит многострочный текст и возвращает все найденные уникальные прокси.
	 *
	 * @param text многострочный текст (например, содержимое файла или буфера обмена)
	 * @return неизменяемое множество найденных прокси
	 */
	public static Set<Proxy> parseAll(String text) {
		if (text == null || text.isBlank()) {
			return Set.of();
		}

		return text.lines()
		           .map(AddressParser::extractProxy)
		           .flatMap(Optional::stream)
		           .collect(Collectors.toUnmodifiableSet());
	}

	private static boolean isValidIp(String ip) {
		if (ip == null) {
			return false;
		}

		String[] octets = ip.split("\\.");

		if (octets.length != IP_OCTET_COUNT) {
			return false;
		}

		return Arrays.stream(octets).allMatch(AddressParser::isValidOctet);
	}

	private static boolean isValidPort(int port) {
		return port >= MIN_PORT && port <= MAX_PORT;
	}

	private static boolean isValidOctet(String octet) {
		try {
			int value = Integer.parseInt(octet);
			return value >= 0 && value <= MAX_OCTET_VALUE;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
}
