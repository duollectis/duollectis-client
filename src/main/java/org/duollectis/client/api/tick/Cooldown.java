package org.duollectis.client.api.tick;

/**
 * Булевый таймер задержки между действиями.
 * <p>
 * Устраняет паттерн {@code if (cooldown > 0) { cooldown--; return; }}.
 * Вызывай {@link #tick()} каждый игровой тик — кулдаун сам отсчитает нужное время.
 * <p>
 * Пример:
 * <pre>{@code
 * private final Cooldown placeCooldown = new Cooldown();
 *
 * // В onTick():
 * placeCooldown.tick();
 *
 * if (placeCooldown.isActive()) return;
 *
 * placeBlock();
 * placeCooldown.set(module.placeDelay.get().intValue());
 * }</pre>
 */
public final class Cooldown {

	private int remaining;

	/**
	 * Продвигает таймер на один тик. Останавливается на нуле.
	 */
	public void tick() {
		if (remaining > 0) {
			remaining--;
		}
	}

	/**
	 * Запускает кулдаун на {@code ticks} тиков.
	 *
	 * @param ticks количество тиков задержки
	 */
	public void set(int ticks) {
		remaining = ticks;
	}

	/**
	 * Возвращает {@code true} если кулдаун ещё активен (действие заблокировано).
	 */
	public boolean isActive() {
		return remaining > 0;
	}

	/**
	 * Возвращает {@code true} если кулдаун истёк (действие разрешено).
	 */
	public boolean isReady() {
		return remaining <= 0;
	}

	/**
	 * Немедленно сбрасывает кулдаун.
	 */
	public void reset() {
		remaining = 0;
	}

	/**
	 * Возвращает оставшееся количество тиков.
	 */
	public int remaining() {
		return remaining;
	}
}
