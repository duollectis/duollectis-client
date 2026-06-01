package org.duollectis.client.api.tick;

/**
 * Счётчик игровых тиков с поддержкой паузы и повтора.
 * <p>
 * Устраняет паттерн ручного ведения {@code int elapsed} и сравнения с порогом.
 * Вызывай {@link #tick()} каждый игровой тик — счётчик сам остановится по достижении лимита.
 * <p>
 * Пример:
 * <pre>{@code
 * private final TickTimer scanTimer = new TickTimer(40);
 *
 * // В onTick():
 * scanTimer.tick();
 *
 * if (scanTimer.isDone()) {
 *     doScan();
 *     scanTimer.reset();
 * }
 * }</pre>
 */
public final class TickTimer {

	private final int limit;
	private int elapsed;

	public TickTimer(int limit) {
		this.limit = limit;
	}

	/**
	 * Продвигает счётчик на один тик. Останавливается по достижении {@code limit}.
	 */
	public void tick() {
		if (elapsed < limit) {
			elapsed++;
		}
	}

	/**
	 * Возвращает {@code true} если счётчик достиг лимита.
	 */
	public boolean isDone() {
		return elapsed >= limit;
	}

	/**
	 * Возвращает {@code true} если счётчик ещё не достиг лимита.
	 */
	public boolean isRunning() {
		return elapsed < limit;
	}

	/**
	 * Сбрасывает счётчик в ноль.
	 */
	public void reset() {
		elapsed = 0;
	}

	/**
	 * Возвращает текущее значение счётчика.
	 */
	public int elapsed() {
		return elapsed;
	}

	/**
	 * Возвращает лимит тиков.
	 */
	public int limit() {
		return limit;
	}
}
