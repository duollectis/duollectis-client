package org.duollectis.client.api.event;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public final class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Event> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners
            .computeIfAbsent(eventType, k -> new ArrayList<>())
            .add((Consumer) listener);
    }

    public <T extends Event> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<Object>> eventListeners = listeners.get(eventType);

        if (eventListeners == null) {
            return;
        }

        eventListeners.remove(listener);
    }

    public <T extends Event> T post(T event) {
        List<Consumer<Object>> eventListeners = listeners.get(event.getClass());

        if (eventListeners == null) {
            return event;
        }

        for (Consumer<Object> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.error("Ошибка в обработчике события {}", event.getClass().getSimpleName(), e);
            }
        }

        return event;
    }

}
