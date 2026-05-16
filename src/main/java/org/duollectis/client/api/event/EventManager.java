package org.duollectis.client.api.event;

import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventManager<T extends EventHandler> implements EventHandler {

	protected final List<T> childs = new ArrayList<>();

	public T add(T handler) {
		childs.add(handler);
		return handler;
	}


	@Override
	public void onTickPre() {
		forEachActive(EventHandler::onTickPre);
	}

	@Override
	public void onTickPost() {
		forEachActive(EventHandler::onTickPost);
	}

	@Override
	public void onPacketReceive(Packet<?> packet, Cancellable cancellable) {
		forEachActive(c -> c.onPacketReceive(packet, cancellable));
	}

	@Override
	public void onPacketSend(Packet<?> packet, Cancellable cancellable) {
		forEachActive(c -> c.onPacketSend(packet, cancellable));
	}

	@Override
	public void onPacketSent(Packet<?> packet) {
		forEachActive(c -> c.onPacketSent(packet));
	}

	@Override
	public void onGameJoined(ServerAddress address, ServerInfo info) {
		forEachActive(c -> c.onGameJoined(address, info));
	}

	@Override
	public void onRender(MatrixStack matrices, GameRenderer gameRenderer, VertexConsumerProvider consumers) {
		forEachActive(c -> c.onRender(matrices, gameRenderer, consumers));
	}

	private void forEachActive(Consumer<EventHandler> consumer) {
		for (EventHandler handler : childs) {
			if (handler.isActive()) {
				consumer.accept(handler);
			}
		}
	}
}
