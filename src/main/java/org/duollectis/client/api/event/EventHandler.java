package org.duollectis.client.api.event;

import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;

public interface EventHandler {

	default void onTickPre() {}
	default void onTickPost() {}
	default void onPacketReceive(Packet<?> packet, Cancellable cancellable) {}
	default void onPacketSend(Packet<?> packet, Cancellable cancellable) {}
	default void onPacketSent(Packet<?> packet) {}
	default void onGameJoined(ServerAddress address, ServerInfo info) {}
	default void onRender(MatrixStack matrices, GameRenderer gameRenderer, VertexConsumerProvider consumers) {}

	default boolean isActive() {return true;}
}
