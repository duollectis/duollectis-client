package org.duollectis.client.api.event.events.input;

import lombok.Getter;
import org.duollectis.client.api.event.CancellableEvent;

/** Событие ввода символа с клавиатуры. Содержит Unicode-кодпоинт и модификаторы. */
@Getter
public final class CharTypeEvent extends CancellableEvent {

	private int codepoint;
	private int rawModifiers;

	public CharTypeEvent set(int codepoint, int rawModifiers) {
		reset();
		this.codepoint = codepoint;
		this.rawModifiers = rawModifiers;
		return this;
	}

	public String asString() {
		return Character.toString(codepoint);
	}
}
