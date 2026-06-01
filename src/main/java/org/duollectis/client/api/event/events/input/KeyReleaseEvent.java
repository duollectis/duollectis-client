package org.duollectis.client.api.event.events.input;

/** Событие отпускания клавиши клавиатуры (GLFW_RELEASE). Не отменяемое. */
public final class KeyReleaseEvent extends AbstractKeyEvent {

	@Override
	public KeyReleaseEvent set(int key, int scancode, int rawModifiers) {
		super.set(key, scancode, rawModifiers);
		return this;
	}
}
