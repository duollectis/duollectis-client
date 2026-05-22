package org.duollectis.client.api.modules;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.duollectis.client.api.event.Event;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.settings.Settings;
import org.duollectis.client.utils.ISerializable;
import org.duollectis.client.utils.ITranslatable;

import static org.duollectis.client.utils.MC.mc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Базовый класс для всех модулей клиента.
 * Управляет жизненным циклом (включение/выключение), автоматически подписывает
 * и отписывает методы, помеченные {@link Subscribe}, от шины событий.
 * Реализует {@link ISerializable} для сохранения состояния и {@link ITranslatable} для локализации.
 */
@Slf4j
public class Module implements ISerializable, ITranslatable {

	private static final String TRANSLATION_PATH = "duollectis.client.modules.";
	private static final int NO_BIND = -1;

	private final List<ListenerEntry<?>> registeredListeners = new ArrayList<>();

	@Getter
	protected final Settings settings = new Settings(this);

	@Getter
	@Setter
	private boolean active;

	@Setter
	private String id;

	@Getter
	@Setter
	private int keyCode = NO_BIND;

	protected boolean debug;

	public Module() {
		scanSubscribeMethods();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getTranslationPath() {
		return TRANSLATION_PATH + id;
	}

	/** Переключает состояние модуля (включён ↔ выключен). */
	public void toggle() {
		if (active) {
			disable();
			return;
		}

		enable();
	}

	/**
	 * Устанавливает конкретное состояние модуля.
	 *
	 * @param shouldBeActive {@code true} — включить, {@code false} — выключить
	 */
	public void toggle(boolean shouldBeActive) {
		if (shouldBeActive) {
			enable();
			return;
		}

		disable();
	}

	/** Включает модуль: подписывает обработчики событий и вызывает {@link #onActivate()}. */
	public void enable() {
		if (active) {
			return;
		}

		active = true;
		onActivate();
		subscribeAll();
		info("Activated");
	}

	/** Выключает модуль: отписывает обработчики событий и вызывает {@link #onDeactivate()}. */
	public void disable() {
		if (active) {
			active = false;
			unsubscribeAll();
			onDeactivate();
			info("Deactivated");
		}
	}

	protected void onActivate() {}

	protected void onDeactivate() {}

	protected <T extends Event> void on(Class<T> eventType, Consumer<T> listener) {
		registeredListeners.add(new ListenerEntry<>(eventType, listener));
	}

	private void subscribeAll() {
		EventBus bus = EventBus.getInstance();

		for (ListenerEntry<?> entry : registeredListeners) {
			entry.subscribe(bus);
		}
	}

	private void unsubscribeAll() {
		EventBus bus = EventBus.getInstance();

		for (ListenerEntry<?> entry : registeredListeners) {
			entry.unsubscribe(bus);
		}
	}

	/**
	 * Сканирует иерархию классов через рефлексию и регистрирует все методы,
	 * помеченные {@link Subscribe}, как обработчики событий.
	 * Тип события определяется по единственному параметру метода.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void scanSubscribeMethods() {
		Class<?> clazz = getClass();

		while (clazz != null && clazz != Object.class) {
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.getAnnotation(Subscribe.class) == null) {
					continue;
				}

				if (method.getParameterCount() != 1) {
					log.warn(
						"Метод @Subscribe '{}' в '{}' должен иметь ровно один параметр — тип события",
						method.getName(), getClass().getSimpleName()
					);
					continue;
				}

				Class<?> paramType = method.getParameterTypes()[0];

				if (!Event.class.isAssignableFrom(paramType)) {
					log.warn(
						"Параметр метода @Subscribe '{}' в '{}' должен реализовывать Event",
						method.getName(), getClass().getSimpleName()
					);
					continue;
				}

				method.setAccessible(true);

				Consumer<Event> listener = event -> {
					try {
						method.invoke(this, event);
					}
					catch (Exception e) {
						log.error(
							"Ошибка в @Subscribe методе '{}' модуля '{}'",
							method.getName(), getClass().getSimpleName(), e
						);
					}
				};

				registeredListeners.add(new ListenerEntry<>((Class) paramType, listener));
			}

			clazz = clazz.getSuperclass();
		}
	}

	/**
	 * Выводит отладочное сообщение в чат, если включён флаг {@code debug}.
	 *
	 * @param arg объект для отображения
	 */
	public void debugInfo(Object arg) {
		if (debug) {
			info(arg);
		}
	}

	/**
	 * Выводит информационное сообщение в чат и отключает модуль.
	 *
	 * @param arg объект для отображения
	 */
	public void infoDisable(Object arg) {
		info(arg);
		disable();
	}

	/**
	 * Выводит предупреждение в чат и отключает модуль.
	 *
	 * @param arg объект для отображения
	 */
	public void warningDisable(Object arg) {
		warning(arg);
		disable();
	}

	/**
	 * Выводит сообщение об ошибке в чат и отключает модуль.
	 *
	 * @param arg объект для отображения
	 */
	public void errorDisable(Object arg) {
		error(arg);
		disable();
	}

	public void info(Object arg) {
		addMessage(tr.get(getName()), arg, 0x60FF60);
	}

	public void warning(Object arg) {
		addMessage(tr.get(getTranslationPath()), arg, 0xFFFF60);
	}

	public void error(Object arg) {
		addMessage(tr.get(getTranslationPath()), arg, 0xFF6060);
	}

	/** Возвращает отображаемое имя назначенной клавиши или {@code null} если бинд не задан. */
	public String getKeyName() {
		if (keyCode == NO_BIND) {
			return null;
		}

		return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
	}

	/** Сбрасывает назначенную клавишу. */
	public void clearBind() {
		keyCode = NO_BIND;
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		JsonObject settingsTag = new JsonObject();

		settings.write(settingsTag);

		json.addProperty("active", active);
		json.addProperty("keyCode", keyCode);
		json.add("settings", settingsTag);

		return json;
	}

	@Override
	public void fromJson(JsonObject json) {
		if (json.has("settings")) {
			settings.read(json.getAsJsonObject("settings"));
		}

		if (json.has("keyCode")) {
			keyCode = json.get("keyCode").getAsInt();
		}

		boolean savedActive = json.has("active") && json.get("active").getAsBoolean();
		boolean currentlyActive = isActive();

		if (savedActive != currentlyActive) {
			toggle();
		}
	}

	private static void addMessage(String name, Object arg, int color) {
		if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null) {
			return;
		}

		mc.inGameHud.getChatHud().addMessage(
				Text.literal("[%s] %s".formatted(name, arg))
				    .setStyle(Style.EMPTY.withColor(color))
		);
	}

	private record ListenerEntry<T extends Event>(Class<T> eventType, Consumer<T> listener) {

		void subscribe(EventBus bus) {
			bus.subscribe(eventType, listener);
		}

		void unsubscribe(EventBus bus) {
			bus.unsubscribe(eventType, listener);
		}

	}

}
