package org.duollectis.client.api.event;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Шина событий — центральный диспетчер для подписки и публикации событий.
 * Реализует паттерн Observer; потокобезопасна благодаря {@link ConcurrentHashMap}.
 * Синглтон: единственный экземпляр доступен через {@link #getInstance()}.
 */
@Slf4j
public final class EventBus {

	private static final EventBus INSTANCE = new EventBus();

	private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

	private EventBus() {}

	public static EventBus getInstance() {
		return INSTANCE;
	}

	/**
	 * Регистрирует обработчик для указанного типа события.
	 * Один и тот же listener можно зарегистрировать несколько раз — он будет вызван столько же раз.
	 *
	 * @param eventType класс события
	 * @param listener  функция-обработчик
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T extends Event> void subscribe(Class<T> eventType, Consumer<T> listener) {
		listeners
			.computeIfAbsent(eventType, k -> new ArrayList<>())
			.add((Consumer) listener);
	}

	/**
	 * Снимает регистрацию обработчика для указанного типа события.
	 * Если обработчик не был зарегистрирован, вызов игнорируется.
	 *
	 * @param eventType класс события
	 * @param listener  ранее зарегистрированный обработчик
	 */
	public <T extends Event> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
		List<Consumer<Object>> eventListeners = listeners.get(eventType);

		if (eventListeners == null) {
			return;
		}

		eventListeners.remove(listener);
	}

	/**
	 * Публикует событие: последовательно вызывает всех зарегистрированных обработчиков.
	 * Исключения в обработчиках логируются и не прерывают цепочку вызовов.
	 *
	 * @param event публикуемое событие
	 * @return то же событие (удобно для проверки флага отмены у {@link CancellableEvent})
	 */
	public <T extends Event> T post(T event) {
		List<Consumer<Object>> eventListeners = listeners.get(event.getClass());

		if (eventListeners == null) {
			return event;
		}

		// Копия списка защищает от ConcurrentModificationException,
		// если обработчик подписывает/отписывает слушателей во время диспетчеризации.
		List<Consumer<Object>> snapshot = new ArrayList<>(eventListeners);

		for (Consumer<Object> listener : snapshot) {
			try {
				listener.accept(event);
			} catch (Exception e) {
				log.error("Ошибка в обработчике события {}", event.getClass().getSimpleName(), e);
			}
		}

		return event;
	}

}
