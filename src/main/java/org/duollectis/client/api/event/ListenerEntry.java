package org.duollectis.client.api.event;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public record ListenerEntry<T extends Event>(Class<T> eventType, Consumer<T> listener) {

	public void subscribe(EventBus bus) {
		bus.subscribe(eventType, listener);
	}

	public void unsubscribe(EventBus bus) {
		bus.unsubscribe(eventType, listener);
	}

	/**
	 * Сканирует иерархию классов через рефлексию и регистрирует все методы,
	 * помеченные {@link Subscribe}, как обработчики событий.
	 * <p>
	 * Тип события определяется двумя способами (в порядке приоритета):
	 * <ol>
	 *   <li>По единственному параметру метода — если метод принимает аргумент типа {@link Event}.</li>
	 *   <li>По атрибуту {@link Subscribe#value()} — если метод без параметров,
	 *       а в аннотации явно указан конкретный подтип события (не сам {@link Event}).</li>
	 * </ol>
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static List<ListenerEntry<?>> scanSubscribeMethods(@NotNull Object o) {
		final Class<?> clazz = o.getClass();
		Class<?> _clazz = clazz;

		List<ListenerEntry<?>> registeredListeners = new ArrayList<>();

		while (_clazz != null && _clazz != Object.class) {
			for (Method method : _clazz.getDeclaredMethods()) {
				Subscribe annotation = method.getAnnotation(Subscribe.class);

				if (annotation == null) {
					continue;
				}

				Class<?> eventType = resolveEventType(method, annotation, clazz);

				if (eventType == null) {
					continue;
				}

				method.setAccessible(true);

				boolean hasParam = method.getParameterCount() == 1;

				Consumer<Event> listener = event -> {
					try {
						if (hasParam) {
							method.invoke(o, event);
						} else {
							method.invoke(o);
						}
					}
					catch (Exception e) {
						log.error(
								"Ошибка в @Subscribe методе '{}' модуля '{}'",
								method.getName(), clazz.getSimpleName(), e
						);
					}
				};

				registeredListeners.add(new ListenerEntry<>((Class) eventType, listener));
			}

			_clazz = _clazz.getSuperclass();
		}

		return registeredListeners;
	}

	private static Class<?> resolveEventType(Method method, Subscribe annotation, Class<?> ownerClass) {
		int paramCount = method.getParameterCount();

		if (paramCount == 1) {
			Class<?> paramType = method.getParameterTypes()[0];

			if (!Event.class.isAssignableFrom(paramType)) {
				log.warn(
						"Параметр метода @Subscribe '{}' в '{}' должен реализовывать Event",
						method.getName(), ownerClass.getSimpleName()
				);
				return null;
			}

			return paramType;
		}

		if (paramCount == 0) {
			Class<? extends Event> annotationType = annotation.value();

			if (annotationType == Event.class) {
				log.warn(
						"Метод @Subscribe '{}' в '{}' без параметров должен указывать тип события в @Subscribe(SomeEvent.class)",
						method.getName(), ownerClass.getSimpleName()
				);
				return null;
			}

			return annotationType;
		}

		log.warn(
				"Метод @Subscribe '{}' в '{}' должен иметь 0 или 1 параметр",
				method.getName(), ownerClass.getSimpleName()
		);
		return null;
	}
}
