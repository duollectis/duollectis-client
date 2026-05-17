package org.duollectis.client.mixin.events.packet;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.duollectis.client.DuollectisMod;
import org.duollectis.client.api.event.Cancellable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

	//	@Inject(
	//			method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
	//			at = @At(
	//					value = "INVOKE",
	//					target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V",
	//					shift = At.Shift.BEFORE
	//			),
	//			cancellable = true
	//	)
	//	private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
	//		if (packet instanceof BundleS2CPacket bundle) {
	//			for (Iterator<Packet<? super ClientPlayPacketListener>> it = bundle.getPackets().iterator();
	//			     it.hasNext(); ) {
	//				if (MeteorClient.EVENT_BUS
	//						.post(new PacketEvent.Receive(it.next(), (ClientConnection) (Object) this))
	//						.isCancelled())
	//					it.remove();
	//			}
	//		}
	//		else if (MeteorClient.EVENT_BUS
	//				.post(new PacketEvent.Receive(packet, (ClientConnection) (Object) this))
	//				.isCancelled())
	//			ci.cancel();
	//	}

	@Inject(
			method = "channelRead0*",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$onPackageReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
		Cancellable cancellable = new Cancellable();
		DuollectisMod.getEventManager().onPacketReceive(packet, cancellable);

		if (cancellable.isCanceled()) {
			ci.cancel();
		}
	}

	@Inject(
			method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$onPackageSend(
			Packet<?> packet,
			@Nullable ChannelFutureListener listener,
			boolean flush,
			CallbackInfo ci
	) {
		Cancellable cancellable = new Cancellable();
		DuollectisMod.getEventManager().onPacketSend(packet, cancellable);

		if (cancellable.isCanceled()) {
			ci.cancel();
		}
	}

	@Inject(
			method = "sendInternal",
			at = @At("HEAD")
	)
	private void duollectis$onPackageSent(
			Packet<?> packet,
			@Nullable ChannelFutureListener listener,
			boolean flush,
			CallbackInfo ci
	) {
		DuollectisMod.getEventManager().onPacketSent(packet);
	}
}
