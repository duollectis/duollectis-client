package org.duollectis.client.api.fsm;

import java.util.EnumMap;
import java.util.Map;

/**
 * Конечный автомат (FSM) с декларативным описанием переходов через fluent API.
 * <p>
 * Устраняет бойлерплейт ручного ведения {@code enum Stage}, {@code switch (stage)},
 * флагов {@code isDone()} и методов {@code reset()} в контроллерах модулей.
 * <p>
 * Пример использования:
 * <pre>{@code
 * private final StateMachine<Stage> fsm = StateMachine
 *     .start(Stage.DONE)
 *     .on(Stage.FLYING,  this::tickFlying)
 *     .on(Stage.WAITING, this::tickWaiting)
 *     .build();
 *
 * // В tick():
 * fsm.tick();
 *
 * // Переход в другое состояние:
 * fsm.go(Stage.WAITING);
 *
 * // Проверка завершения:
 * if (fsm.is(Stage.DONE)) { ... }
 *
 * // Сброс к начальному состоянию:
 * fsm.reset();
 *
 * // Перезапуск с нового состояния:
 * fsm.restart(Stage.FLYING);
 * }</pre>
 *
 * @param <S> тип перечисления состояний
 */
public final class StateMachine<S extends Enum<S>> {

	private final Map<S, Runnable> handlers;
	private final S initialState;
	private S current;

	private StateMachine(S initialState, Map<S, Runnable> handlers) {
		this.initialState = initialState;
		this.current = initialState;
		this.handlers = handlers;
	}

	/**
	 * Выполняет обработчик текущего состояния.
	 * Если для текущего состояния обработчик не зарегистрирован — ничего не происходит
	 * (терминальные состояния намеренно не имеют обработчика).
	 */
	public void tick() {
		Runnable handler = handlers.get(current);

		if (handler != null) {
			handler.run();
		}
	}

	/**
	 * Переводит автомат в указанное состояние.
	 *
	 * @param next целевое состояние
	 */
	public void go(S next) {
		current = next;
	}

	/**
	 * Переводит автомат в указанное состояние, если выполнено условие.
	 *
	 * @param condition условие перехода
	 * @param next      целевое состояние
	 */
	public void goIf(boolean condition, S next) {
		if (condition) {
			current = next;
		}
	}

	/**
	 * Возвращает {@code true} если автомат находится в указанном состоянии.
	 */
	public boolean is(S state) {
		return current == state;
	}

	/**
	 * Возвращает текущее состояние.
	 */
	public S state() {
		return current;
	}

	/**
	 * Сбрасывает автомат к начальному состоянию, заданному при построении.
	 */
	public void reset() {
		current = initialState;
	}

	/**
	 * Переводит автомат в указанное состояние без изменения {@code initialState}.
	 * Удобно для перезапуска с нового стартового состояния.
	 *
	 * @param startFrom состояние, с которого начать
	 */
	public void restart(S startFrom) {
		current = startFrom;
	}

	/**
	 * Начинает построение автомата с указанным начальным (и сбросным) состоянием.
	 *
	 * @param initialState начальное состояние
	 * @param <S>          тип перечисления состояний
	 * @return билдер для регистрации обработчиков
	 */
	public static <S extends Enum<S>> Builder<S> start(S initialState) {
		return new Builder<>(initialState);
	}

	/**
	 * Билдер конечного автомата.
	 *
	 * @param <S> тип перечисления состояний
	 */
	public static final class Builder<S extends Enum<S>> {

		private final S initialState;
		private final Map<S, Runnable> handlers;

		@SuppressWarnings("unchecked")
		private Builder(S initialState) {
			this.initialState = initialState;
			this.handlers = new EnumMap<>((Class<S>) initialState.getClass());
		}

		/**
		 * Регистрирует обработчик для состояния.
		 * Обработчик вызывается каждый тик пока автомат находится в этом состоянии.
		 * Для перехода в другое состояние обработчик должен вызвать {@link StateMachine#go}.
		 *
		 * @param state   состояние
		 * @param handler действие, выполняемое каждый тик в этом состоянии
		 * @return этот билдер
		 */
		public Builder<S> on(S state, Runnable handler) {
			handlers.put(state, handler);
			return this;
		}

		/**
		 * Объявляет терминальное состояние — без обработчика.
		 * {@link StateMachine#tick()} в этом состоянии ничего не делает.
		 * Вызов необязателен: любое незарегистрированное состояние уже является терминальным.
		 * Используется для явной документации намерения в коде.
		 *
		 * @param state терминальное состояние
		 * @return этот билдер
		 */
		public Builder<S> terminal(S state) {
			handlers.remove(state);
			return this;
		}

		/**
		 * Создаёт и возвращает готовый {@link StateMachine}.
		 */
		public StateMachine<S> build() {
			return new StateMachine<>(initialState, handlers);
		}
	}
}
