package org.duollectis.client.api.event.events.input;

import lombok.Getter;
import org.duollectis.client.api.event.Event;

/** Событие перемещения курсора мыши. */
@Getter
public final class MouseMoveEvent implements Event {

	public static final MouseMoveEvent INSTANCE = new MouseMoveEvent();

	private double x;
	private double y;

	public MouseMoveEvent set(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}
}
