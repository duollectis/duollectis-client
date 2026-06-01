package org.duollectis.client.api.event.events;

import lombok.Getter;
import net.minecraft.network.packet.Packet;
import org.duollectis.client.api.event.Event;

/** Событие успешной отправки пакета на сервер (после фактической передачи в канал). */
@Getter
public final class PacketSentEvent implements Event {

	public static final PacketSentEvent INSTANCE = new PacketSentEvent();

	private Packet<?> packet;

	private PacketSentEvent() {}

	public void set(Packet<?> packet) {
		this.packet = packet;
	}

}
