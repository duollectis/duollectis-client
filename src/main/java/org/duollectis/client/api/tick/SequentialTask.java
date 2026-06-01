package org.duollectis.client.api.tick;

import java.util.function.BooleanSupplier;

/**
 * Линейная цепочка шагов для простых контроллеров без полноценного FSM.
 * <p>
 * Каждый шаг — это {@link BooleanSupplier}: возвращает {@code true} когда шаг завершён
 * и можно переходить к следующему, {@code false} — шаг ещё выполняется.
 * <p>
 * Пример:
 * <pre>{@code
 * private final SequentialTask task = SequentialTask.of(
 *     this::stepFly,      // true когда долетели
 *     this::stepOpen,     // true когда открыли контейнер
 *     this::stepCollect,  // true когда собрали предметы
 *     this::stepClose     // true когда закрыли
 * );
 *
 * // В onTick():
 * task.tick();
 *
 * if (task.isDone()) {
 *     // все шаги выполнены
 * }
 *
 * // Сброс для повторного запуска:
 * task.reset();
 * }</pre>
 */
public final class SequentialTask {

	private final BooleanSupplier[] steps;
	private int currentStep;

	private SequentialTask(BooleanSupplier[] steps) {
		this.steps = steps;
	}

	/**
	 * Создаёт задачу из последовательности шагов.
	 *
	 * @param steps шаги в порядке выполнения; каждый возвращает {@code true} при завершении
	 * @return новая задача
	 */
	public static SequentialTask of(BooleanSupplier... steps) {
		return new SequentialTask(steps);
	}

	/**
	 * Выполняет текущий шаг.
	 * Если шаг вернул {@code true} — переходит к следующему.
	 * Если все шаги завершены — ничего не делает.
	 */
	public void tick() {
		if (currentStep >= steps.length) {
			return;
		}

		boolean stepDone = steps[currentStep].getAsBoolean();

		if (stepDone) {
			currentStep++;
		}
	}

	/**
	 * Возвращает {@code true} если все шаги выполнены.
	 */
	public boolean isDone() {
		return currentStep >= steps.length;
	}

	/**
	 * Возвращает индекс текущего шага (0-based).
	 */
	public int currentStep() {
		return currentStep;
	}

	/**
	 * Сбрасывает задачу к первому шагу для повторного запуска.
	 */
	public void reset() {
		currentStep = 0;
	}

	/**
	 * Принудительно переходит к шагу с указанным индексом.
	 *
	 * @param step индекс шага (0-based)
	 */
	public void jumpTo(int step) {
		currentStep = Math.max(0, Math.min(step, steps.length));
	}
}
