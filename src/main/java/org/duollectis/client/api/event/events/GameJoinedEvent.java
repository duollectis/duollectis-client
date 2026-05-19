package org.duollectis.client.api.event.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.duollectis.client.api.event.Event;

@Getter
@RequiredArgsConstructor
public final class GameJoinedEvent implements Event {

    private final ServerAddress address;
    private final ServerInfo info;

}
