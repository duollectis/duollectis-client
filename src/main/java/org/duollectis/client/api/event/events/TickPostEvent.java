package org.duollectis.client.api.event.events;

import org.duollectis.client.api.event.Event;

/** Событие завершения игрового тика (публикуется в TAIL метода {@code MinecraftClient#tick}). */
public final class TickPostEvent implements Event {

	public static final TickPostEvent INSTANCE = new TickPostEvent();

	private TickPostEvent() {}

}
