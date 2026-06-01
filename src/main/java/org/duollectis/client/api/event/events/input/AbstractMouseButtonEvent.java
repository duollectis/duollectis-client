package org.duollectis.client.api.event.events.input;

import lombok.Getter;
import org.duollectis.client.api.event.CancellableEvent;

/**
 * Базовый класс для событий кнопок мыши.
 * Содержит общие поля: кнопку и модификаторы.
 * Наследует {@link CancellableEvent} — отмена используется только в {@link MouseButtonEvent}.
 */
@Getter
public abstract class AbstractMouseButtonEvent extends CancellableEvent {

	private int button;
	private int rawModifiers;

	protected AbstractMouseButtonEvent set(int button, int rawModifiers) {
		reset();
		this.button = button;
		this.rawModifiers = rawModifiers;
		return this;
	}

	public boolean hasModifier(InputModifier modifier) {
		return modifier.isActive(rawModifiers);
	}
}
