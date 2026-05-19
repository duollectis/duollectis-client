package org.duollectis.client.api.menu.proxy;

import org.duollectis.client.api.proxy.Proxy;

import java.util.List;
import java.util.function.Predicate;

/**
 * Filter modes for the unified proxy list.
 * Add new values here — the dropdown will pick them up automatically.
 */
enum ProxyFilter {

	ALL(p -> true),
	UNKNOWN(p -> p.getStatus() == Proxy.Status.UNKNOWN),
	CHECKING(p -> p.getStatus() == Proxy.Status.CHECKING),
	FAILED(p -> p.getStatus() == Proxy.Status.FAILED
	         || p.getStatus() == Proxy.Status.DEAD_IP
	         || p.getStatus() == Proxy.Status.DEAD_SERVER
	         || p.getStatus() == Proxy.Status.PING_ERROR),
	OK(p -> p.getStatus() == Proxy.Status.OK);

	private final Predicate<Proxy> predicate;

	ProxyFilter(Predicate<Proxy> predicate) {
		this.predicate = predicate;
	}

	List<Proxy> apply(List<Proxy> list) {
		return list.stream().filter(predicate).toList();
	}
}
