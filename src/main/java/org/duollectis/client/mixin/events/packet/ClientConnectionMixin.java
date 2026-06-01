package org.duollectis.client.mixin.events.packet;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.PacketReceiveEvent;
import org.duollectis.client.api.event.events.PacketSendEvent;
import org.duollectis.client.api.event.events.PacketSentEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для {@link ClientConnection}: перехватывает входящие
 * и исходящие пакеты и публикует соответствующие события шины ({@link PacketReceiveEvent},
 * {@link PacketSendEvent}, {@link PacketSentEvent}).
 */
@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

	@Inject(
			method = "channelRead0*",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
		PacketReceiveEvent.INSTANCE.reset();
		PacketReceiveEvent.INSTANCE.set(packet);
		EventBus.getInstance().post(PacketReceiveEvent.INSTANCE);

		if (PacketReceiveEvent.INSTANCE.isCancelled()) {
			ci.cancel();
		}
	}

	@Inject(
			method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$onPacketSend(
			Packet<?> packet,
			@Nullable ChannelFutureListener listener,
			boolean flush,
			CallbackInfo ci
	) {
		PacketSendEvent.INSTANCE.reset();
		PacketSendEvent.INSTANCE.set(packet);
		EventBus.getInstance().post(PacketSendEvent.INSTANCE);

		if (PacketSendEvent.INSTANCE.isCancelled()) {
			ci.cancel();
		}
	}

	@Inject(
			method = "sendInternal",
			at = @At("HEAD")
	)
	private void duollectis$onPacketSent(
			Packet<?> packet,
			@Nullable ChannelFutureListener listener,
			boolean flush,
			CallbackInfo ci
	) {
		PacketSentEvent.INSTANCE.set(packet);
		EventBus.getInstance().post(PacketSentEvent.INSTANCE);
	}

}
