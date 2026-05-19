package org.duollectis.client.api.menu.proxy;

import lombok.Getter;
import org.duollectis.client.api.proxy.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public enum SortMode {
    NONE("screen.duollectis.proxy_manager.sort.none", null),
    LATENCY_ASC("screen.duollectis.proxy_manager.sort.latency_asc", Comparator.comparingLong(Proxy::getLatency)),
    LATENCY_DESC("screen.duollectis.proxy_manager.sort.latency_desc", Comparator.comparingLong(Proxy::getLatency).reversed()),
    PING_ASC("screen.duollectis.proxy_manager.sort.ping_asc", Comparator.comparing(Proxy::getPingCount)),
    PING_DESC("screen.duollectis.proxy_manager.sort.ping_desc", Comparator.comparing(Proxy::getPingCount).reversed()),
    TYPE("screen.duollectis.proxy_manager.sort.type", Comparator.comparing(p -> p.getType() != null ? p.getType().name() : "")),
    IP("screen.duollectis.proxy_manager.sort.ip", Comparator.comparing(p -> p.getAddress() != null ? p.getAddress() : ""));

    @Getter
    @NotNull
    private final String translation;

    @Nullable
    private final Comparator<Proxy> comparator;

    SortMode(@NotNull String translation, @Nullable Comparator<Proxy> comparator) {
        this.translation = translation;
        this.comparator = comparator;
    }

    List<Proxy> apply(List<Proxy> list) {
        if (comparator == null) {
            return list;
        }

        return list.stream().sorted(comparator).toList();
    }
}
