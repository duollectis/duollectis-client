package org.duollectis.client.api.event.events.input;

/** Событие нажатия клавиши клавиатуры (GLFW_PRESS). Отменяемое. */
public final class KeyPressEvent extends AbstractKeyEvent {

	@Override
	public KeyPressEvent set(int key, int scancode, int rawModifiers) {
		super.set(key, scancode, rawModifiers);
		return this;
	}
}
