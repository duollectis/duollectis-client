package org.duollectis.client.api.tick;

/**
 * Ватчдог для повторных попыток открытия контейнера.
 * <p>
 * Инкапсулирует паттерн «подождать N тиков → повторить → сдаться после MAX попыток»
 * <p>
 * Использование:
 * <pre>{@code
 * private final OpenWatchdog watchdog = new OpenWatchdog(MAX_ATTEMPTS, openTimeout);
 *
 * // В onTick, пока контейнер не открылся:
 * watchdog.tick(
 *     this::sendOpenPacket,   // onRetry — повторить клик
 *     this::giveUp            // onGiveUp — превышен лимит попыток
 * );
 *
 * // Когда контейнер открылся:
 * watchdog.reset();
 * }</pre>
 */
public final class OpenWatchdog {

	private final int maxAttempts;
	private final int timeoutTicks;

	private int elapsed;
	private int attempts;

	public OpenWatchdog(int maxAttempts, int timeoutTicks) {
		this.maxAttempts = maxAttempts;
		this.timeoutTicks = timeoutTicks;
	}

	/**
	 * Вызывается каждый тик, пока контейнер ещё не открылся.
	 * Когда истекает {@code timeoutTicks} тиков — либо повторяет попытку,
	 * либо вызывает {@code onGiveUp} если исчерпан лимит.
	 *
	 * @param onRetry  действие повторной попытки (отправить пакет открытия)
	 * @param onGiveUp действие при исчерпании всех попыток
	 * @return {@code true} если лимит исчерпан и был вызван {@code onGiveUp}
	 */
	public boolean tick(Runnable onRetry, Runnable onGiveUp) {
		elapsed++;

		if (elapsed < timeoutTicks) {
			return false;
		}

		elapsed = 0;

		if (attempts >= maxAttempts) {
			onGiveUp.run();
			return true;
		}

		attempts++;
		onRetry.run();
		return false;
	}

	public void reset() {
		elapsed = 0;
		attempts = 0;
	}

	public int attempts() {
		return attempts;
	}

	public int maxAttempts() {
		return maxAttempts;
	}
}
