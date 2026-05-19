package org.duollectis.client.api.menu.proxy;

import org.duollectis.client.api.proxy.Proxy;
import org.jetbrains.annotations.Nullable;

/**
 * A single row in the proxy list.
 * <p>
 * For single-type proxies: {@code parent} is the proxy, {@code childType} is null.
 * For dual-type parent rows: {@code parent} is the proxy, {@code childType} is null,
 * {@code isDualParent} is true.
 * For child rows (expanded dual-type): {@code parent} is the owning proxy,
 * {@code childType} is the specific type for this child row.
 */
record ProxyListEntry(
		Proxy parent,
		@Nullable Proxy.Type childType,
		boolean isDualParent
) {

	/**
	 * Convenience factory for a normal single-type row.
	 */
	static ProxyListEntry single(Proxy p) {
		return new ProxyListEntry(p, null, false);
	}

	/**
	 * Convenience factory for a dual-type parent row.
	 */
	static ProxyListEntry dualParent(Proxy p) {
		return new ProxyListEntry(p, null, true);
	}

	/**
	 * Convenience factory for a child row.
	 */
	static ProxyListEntry child(Proxy p, Proxy.Type type) {
		return new ProxyListEntry(p, type, false);
	}

	/**
	 * True if this is a child row (has a specific type override).
	 */
	boolean isChild() {
		return childType != null;
	}

	/**
	 * The effective type for display/connection purposes.
	 * For child rows: the child's specific type.
	 * For all others: the proxy's own type.
	 */
	@Nullable Proxy.Type effectiveType() {
		return childType != null ? childType : parent.getType();
	}
}
