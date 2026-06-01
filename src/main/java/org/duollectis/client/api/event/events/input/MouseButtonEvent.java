package org.duollectis.client.api.event.events.input;

/** Событие нажатия кнопки мыши (GLFW_PRESS). Отменяемое. */
public final class MouseButtonEvent extends AbstractMouseButtonEvent {

	public static final MouseButtonEvent INSTANCE = new MouseButtonEvent();

	@Override
	public MouseButtonEvent set(int button, int rawModifiers) {
		super.set(button, rawModifiers);
		return this;
	}
}
