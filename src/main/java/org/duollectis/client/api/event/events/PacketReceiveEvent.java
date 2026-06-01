package org.duollectis.client.api.event.events;

import lombok.Getter;
import net.minecraft.network.packet.Packet;
import org.duollectis.client.api.event.CancellableEvent;

/** Отменяемое событие получения входящего пакета от сервера. */
@Getter
public final class PacketReceiveEvent extends CancellableEvent {

	public static final PacketReceiveEvent INSTANCE = new PacketReceiveEvent();

	private Packet<?> packet;

	private PacketReceiveEvent() {}

	public void set(Packet<?> packet) {
		this.packet = packet;
	}

}
