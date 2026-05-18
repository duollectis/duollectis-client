package org.duollectis.client.api.menu.proxy;

import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static org.duollectis.client.api.menu.proxy.ProxyScreenKeys.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;

/**
 * Sort mode enum and helpers for the OK (right) proxy list.
 * <p>Extracted from {@link ProxyScreen} to keep sort logic self-contained.
 */
final class ProxyOkSorter {

	private ProxyOkSorter() {}

	enum Mode {
		NONE(SORT_NONE, null),
		LATENCY_ASC(SORT_LAT_ASC, Comparator.comparingLong(Proxy::getLatency)),
		LATENCY_DESC(SORT_LAT_DESC, Comparator.comparingLong(Proxy::getLatency).reversed()),
		PING_COUNT(SORT_PING_COUNT, Comparator.comparing(Proxy::getPingCount)),
		TYPE(SORT_TYPE, Comparator.comparing(p -> p.getType() != null ? p.getType().name() : "")),
		IP(SORT_IP, Comparator.comparing(p -> p.getAddress() != null ? p.getAddress() : ""));

		@NotNull
		private final String translation;

		@Nullable
		private final Comparator<Proxy> comparator;

		Mode(@NotNull String translation, @Nullable Comparator<Proxy> comparator) {
			this.translation = translation;
			this.comparator = comparator;
		}
	}

	/**
	 * Returns the display label for the sort button given the current mode.
	 */
	static Text sortLabel(Mode mode) {
		return Text.literal(ts(BTN_SORT) + ts(mode.translation));
	}

	/**
	 * Returns the next sort mode in the cycle.
	 */
	static Mode cycle(Mode mode) {
		if (mode.ordinal() >= Mode.values().length - 1) {
			return Mode.values()[0];
		}

		return Mode.values()[mode.ordinal() + 1];
	}

	/**
	 * Returns {@code list} sorted according to {@code mode} (new list, original unchanged).
	 */
	static List<Proxy> apply(List<Proxy> list, Mode mode) {
		if (mode.comparator == null) {
			return list;
		}

		return list.stream().sorted(mode.comparator).toList();
	}
}
