package org.duollectis.client.api.event.events.input;

import lombok.Getter;
import org.duollectis.client.api.event.CancellableEvent;

/**
 * Базовый класс для всех событий клавиатуры.
 * Содержит общие поля: клавишу, scancode и модификаторы.
 * Наследует {@link CancellableEvent} — отмена используется только в {@link KeyPressEvent}.
 */
@Getter
public abstract class AbstractKeyEvent extends CancellableEvent {

	private int key;
	private int scancode;
	private int rawModifiers;

	protected AbstractKeyEvent set(int key, int scancode, int rawModifiers) {
		reset();
		this.key = key;
		this.scancode = scancode;
		this.rawModifiers = rawModifiers;
		return this;
	}

	public boolean hasModifier(InputModifier modifier) {
		return modifier.isActive(rawModifiers);
	}
}
