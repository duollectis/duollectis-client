package org.duollectis.client.api.tick;

import org.jetbrains.annotations.Nullable;

/**
 * Таймер ожидания с колбэком на таймаут.
 * <p>
 * Устраняет паттерн «жди N тиков, если не дождался — выполни действие».
 * Типичный пример: ожидание открытия шалкера — если сервер не ответил за {@code openTimeout} тиков,
 * повторяем клик.
 * <p>
 * Пример:
 * <pre>{@code
 * private final Timeout openTimeout = new Timeout();
 *
 * // Запуск ожидания:
 * openTimeout.start(module.openTimeout.get().intValue(), this::retryOpen);
 *
 * // В onTick():
 * openTimeout.tick();
 *
 * // Отмена при успехе:
 * openTimeout.cancel();
 * }</pre>
 */
public final class Timeout {

	private int remaining;
	@Nullable
	private Runnable onTimeout;

	/**
	 * Запускает таймер ожидания.
	 * Если таймер уже запущен — перезапускает с новыми параметрами.
	 *
	 * @param ticks     количество тиков до таймаута
	 * @param onTimeout действие, выполняемое при истечении времени
	 */
	public void start(int ticks, Runnable onTimeout) {
		this.remaining = ticks;
		this.onTimeout = onTimeout;
	}

	/**
	 * Продвигает таймер на один тик.
	 * При достижении нуля вызывает колбэк {@code onTimeout} и сбрасывает таймер.
	 */
	public void tick() {
		if (remaining <= 0 || onTimeout == null) {
			return;
		}

		remaining--;

		if (remaining <= 0) {
			Runnable callback = onTimeout;
			onTimeout = null;
			callback.run();
		}
	}

	/**
	 * Отменяет таймер без вызова колбэка.
	 * Используется при успешном завершении ожидаемого события.
	 */
	public void cancel() {
		remaining = 0;
		onTimeout = null;
	}

	/**
	 * Возвращает {@code true} если таймер активен (ожидание не завершено).
	 */
	public boolean isActive() {
		return remaining > 0 && onTimeout != null;
	}

	/**
	 * Возвращает оставшееся количество тиков до таймаута.
	 */
	public int remaining() {
		return remaining;
	}
}
