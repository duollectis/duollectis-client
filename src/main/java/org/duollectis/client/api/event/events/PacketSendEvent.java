package org.duollectis.client.api.event.events;

import lombok.Getter;
import net.minecraft.network.packet.Packet;
import org.duollectis.client.api.event.CancellableEvent;

/** Отменяемое событие отправки исходящего пакета на сервер (до фактической отправки). */
@Getter
public final class PacketSendEvent extends CancellableEvent {

	public static final PacketSendEvent INSTANCE = new PacketSendEvent();

	private Packet<?> packet;

	private PacketSendEvent() {}

	public void set(Packet<?> packet) {
		this.packet = packet;
	}

}
