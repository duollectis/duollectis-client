package org.duollectis.client.api.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Proxy {

	private final String name;

	private final String address;
	private final int port;
	private final Type type;
	private final String username;
	private final String password;

	public enum Type {
		SOCKS4,
		SOCKS5,
	}
}
