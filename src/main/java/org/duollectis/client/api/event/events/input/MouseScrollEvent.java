package org.duollectis.client.api.event.events.input;

import lombok.Getter;
import org.duollectis.client.api.event.CancellableEvent;

/** Событие прокрутки колёсика мыши. */
@Getter
public final class MouseScrollEvent extends CancellableEvent {

	public static final MouseScrollEvent INSTANCE = new MouseScrollEvent();

	private double horizontal;
	private double vertical;

	public MouseScrollEvent set(double horizontal, double vertical) {
		reset();
		this.horizontal = horizontal;
		this.vertical = vertical;
		return this;
	}
}
