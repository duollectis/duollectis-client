package org.duollectis.client.api.event;

import lombok.Getter;

/**
 * Базовый класс для отменяемых событий.
 * Если обработчик вызывает {@link #cancel()}, Mixin-инжект отменяет оригинальный вызов.
 */
public abstract class CancellableEvent implements Event {

	@Getter
	private boolean cancelled;

	public void cancel() {
		cancelled = true;
	}

	public void reset() {
		cancelled = false;
	}

}
