package org.duollectis.client.api.event.events;

import org.duollectis.client.api.event.Event;

/** Событие начала игрового тика (публикуется в HEAD метода {@link net.minecraft.client.MinecraftClient#tick}). */
public final class TickPreEvent implements Event {

	public static final TickPreEvent INSTANCE = new TickPreEvent();

	private TickPreEvent() {}

}
