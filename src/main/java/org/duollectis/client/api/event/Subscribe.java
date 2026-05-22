package org.duollectis.client.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает метод как обработчик события шины {@link EventBus}.
 * Метод должен принимать ровно один параметр — тип конкретного события.
 * {@link Module} автоматически регистрирует все такие методы при включении.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {}
