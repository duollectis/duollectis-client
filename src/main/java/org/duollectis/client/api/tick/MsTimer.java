package org.duollectis.client.api.tick;

/**
 * Таймер на основе реального времени (миллисекунды).
 * <p>
 * В отличие от {@link Cooldown} и {@link TickTimer}, не зависит от игровых тиков —
 * работает на {@code System.nanoTime()} и корректен даже при лагах сервера или просадках TPS.
 * Используй для задержек, которые должны быть точными по реальному времени:
 * анти-спам кликов, rate-limiting пакетов, UI-анимации.
 * <p>
 * Пример:
 * <pre>{@code
 * private final MsTimer clickTimer = new MsTimer();
 *
 * // Проверка перед действием:
 * if (clickTimer.hasElapsed(250)) {
 *     sendClick();
 *     clickTimer.reset();
 * }
 * }</pre>
 * Или через явный запуск:
 * <pre>{@code
 * private final MsTimer openTimer = new MsTimer();
 *
 * openTimer.start(500); // ждём 500 мс
 *
 * if (openTimer.isDone()) {
 *     retryOpen();
 *     openTimer.start(500);
 * }
 * }</pre>
 */
public final class MsTimer {

	private static final long NANOS_PER_MS = 1_000_000L;

	private long startNanos = System.nanoTime();
	private long limitNanos = 0;

	/**
	 * Запускает таймер с указанным лимитом в миллисекундах.
	 * Сбрасывает точку отсчёта на текущий момент.
	 *
	 * @param ms лимит в миллисекундах
	 */
	public void start(long ms) {
		startNanos = System.nanoTime();
		limitNanos = ms * NANOS_PER_MS;
	}

	/**
	 * Сбрасывает точку отсчёта на текущий момент без изменения лимита.
	 * Удобно для повторного запуска с тем же интервалом.
	 */
	public void reset() {
		startNanos = System.nanoTime();
	}

	/**
	 * Возвращает {@code true} если с момента последнего {@link #start} или {@link #reset}
	 * прошло не менее {@code ms} миллисекунд.
	 *
	 * @param ms порог в миллисекундах
	 */
	public boolean hasElapsed(long ms) {
		return System.nanoTime() - startNanos >= ms * NANOS_PER_MS;
	}

	/**
	 * Возвращает {@code true} если лимит, заданный в {@link #start}, истёк.
	 * Если {@link #start} не вызывался — всегда возвращает {@code true}.
	 */
	public boolean isDone() {
		return System.nanoTime() - startNanos >= limitNanos;
	}

	/**
	 * Возвращает количество миллисекунд, прошедших с последнего {@link #start} или {@link #reset}.
	 */
	public long elapsedMs() {
		return (System.nanoTime() - startNanos) / NANOS_PER_MS;
	}

	/**
	 * Возвращает оставшееся время до лимита в миллисекундах.
	 * Возвращает {@code 0} если лимит уже истёк.
	 */
	public long remainingMs() {
		long elapsed = System.nanoTime() - startNanos;
		long remaining = limitNanos - elapsed;
		return remaining > 0 ? remaining / NANOS_PER_MS : 0;
	}
}
