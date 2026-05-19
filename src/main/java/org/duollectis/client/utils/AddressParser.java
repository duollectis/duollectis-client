package org.duollectis.client.utils;

import lombok.experimental.UtilityClass;
import org.duollectis.client.api.proxy.Proxy;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public class AddressParser {

	private final int MIN_PORT = 1;
	private final int MAX_PORT = 65535;
	private final int IP_OCTET_COUNT = 4;
	private final int MAX_OCTET_VALUE = 255;

	private final Pattern IP_PORT_PATTERN = Pattern.compile(
			"\\b(?<ip>\\d{1,3}(?:\\.\\d{1,3}){3})\\s*[:\\s]\\s*(?<port>\\d{1,5})\\b"
	);

	public Optional<Proxy> extractProxy(String line) {
		if (line == null || line.isBlank()) {
			return Optional.empty();
		}

		Matcher matcher = IP_PORT_PATTERN.matcher(line.trim());

		if (matcher.find() == false) {
			return Optional.empty();
		}

		String ip = matcher.group(1);
		int port = Integer.parseInt(matcher.group(2));

		return isValidIp(ip) && isValidPort(port)
		       ? Optional.of(new Proxy(ip, port))
		       : Optional.empty();
	}

	public Set<Proxy> parseAll(String text) {
		if (text == null || text.isBlank()) {
			return Set.of();
		}

		return text.lines()
		           .map(AddressParser::extractProxy)
		           .flatMap(Optional::stream)
		           .collect(Collectors.toUnmodifiableSet());
	}

	private boolean isValidIp(String ip) {
		if (ip == null) {
			return false;
		}

		String[] octets = ip.split("\\.");

		if (octets.length != IP_OCTET_COUNT) {
			return false;
		}

		return Arrays.stream(octets).allMatch(AddressParser::isValidOctet);
	}

	private boolean isValidPort(int port) {
		return port >= MIN_PORT && port <= MAX_PORT;
	}

	private boolean isValidOctet(String octet) {
		try {
			int value = Integer.parseInt(octet);
			return value >= 0 && value <= MAX_OCTET_VALUE;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
}
