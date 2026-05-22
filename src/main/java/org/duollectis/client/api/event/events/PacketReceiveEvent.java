package org.duollectis.client.api.event.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.packet.Packet;
import org.duollectis.client.api.event.CancellableEvent;

/** Отменяемое событие получения входящего пакета от сервера. */
@Getter
@RequiredArgsConstructor
public final class PacketReceiveEvent extends CancellableEvent {

	private final Packet<?> packet;

}
