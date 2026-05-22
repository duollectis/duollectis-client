package org.duollectis.client.api.event.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.packet.Packet;
import org.duollectis.client.api.event.Event;

/** Событие успешной отправки пакета на сервер (после фактической передачи в канал). */
@Getter
@RequiredArgsConstructor
public final class PacketSentEvent implements Event {

	private final Packet<?> packet;

}
