package org.duollectis.client.api.modules;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.duollectis.client.api.ILoggable;
import org.duollectis.client.api.bind.BindManager;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.ListenerEntry;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.settings.Settings;
import org.duollectis.client.utils.ISerializable;
import org.duollectis.client.utils.ITranslatable;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс для всех модулей клиента.
 * Управляет жизненным циклом (включение/выключение), автоматически подписывает
 * и отписывает методы, помеченные {@link Subscribe}, от шины событий.
 * Реализует {@link ISerializable} для сохранения состояния и {@link ITranslatable} для локализации.
 */
@Slf4j
public class Module extends ControllerNode implements ISerializable, ITranslatable, ILoggable {

	protected static final MinecraftClient mc = MinecraftClient.getInstance();

	private static final String TRANSLATION_PATH = "duollectis.client.modules.";

	private final List<ListenerEntry<?>> registeredListeners = new ArrayList<>();

	@Getter
	protected final Settings settings = new Settings(this);

	@Getter
	@Setter
	private boolean active;

	@Setter
	private String id;

	protected boolean debug;

	public Module() {
		registeredListeners.addAll(collectAllListeners());
	}

	protected void onInit() {}

	@SuppressWarnings("unchecked")
	protected <M extends Module, C extends Controller<M>> void addController(C controller) {
		addController((M) this, controller);
		registeredListeners.addAll(controller.collectAllListeners());
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getTranslationPath() {
		return TRANSLATION_PATH + id;
	}

	/**
	 * Переключает состояние модуля (включён ↔ выключен).
	 */
	public void toggle() {
		if (active) {
			disable();
		}
		else {
			enable();
		}
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

	/**
	 * Включает модуль: подписывает обработчики событий и вызывает {@link #onActivate()},
	 * затем каскадно активирует все дочерние контроллеры.
	 */
	@Override
	public void enable() {
		if (active) {
			return;
		}

		active = true;
		super.enable();
		subscribeAll();
		info("Activated");
	}

	/**
	 * Выключает модуль: отписывает обработчики событий и вызывает {@link #onDeactivate()},
	 * затем каскадно деактивирует все дочерние контроллеры.
	 */
	@Override
	public void disable() {
		if (active) {
			active = false;
			unsubscribeAll();
			super.disable();
			info("Deactivated");
		}
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

	@Override
	public void info(Object arg) {
		addMessage(getName(), arg, 0x60FF60);
	}

	@Override
	public void warning(Object arg) {
		addMessage(getTranslationPath(), arg, 0xFFFF60);
	}

	@Override
	public void error(Object arg) {
		addMessage(getTranslationPath(), arg, 0xFF6060);
	}

	/**
	 * Выводит отладочное сообщение в чат, если включён флаг {@code debug}.
	 *
	 * @param arg объект для отображения
	 */
	@Override
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

	/** Возвращает отображаемое имя назначенной клавиши или {@code null} если бинд не задан. */
	public String getKeyName() {
		return BindManager.getInstance().getModuleKeyName(id);
	}

	/** Возвращает keyCode GLFW назначенной клавиши или -1 если бинд не задан. */
	public int getKeyCode() {
		return BindManager.getInstance().getModuleKey(id);
	}

	/** Устанавливает клавишу бинда. */
	public void setKeyCode(int keyCode) {
		BindManager.getInstance().setModuleKey(id, keyCode);
	}

	/** Сбрасывает назначенную клавишу. */
	public void clearBind() {
		BindManager.getInstance().clearModuleKey(id);
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		JsonObject settingsTag = new JsonObject();

		settings.write(settingsTag);

		json.addProperty("active", active);
		json.add("settings", settingsTag);

		return json;
	}

	@Override
	public void fromJson(JsonObject json) {
		if (json.has("settings")) {
			settings.read(json.getAsJsonObject("settings"));
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

		mc.execute(() -> mc.inGameHud.getChatHud().addMessage(
				Text.literal("[%s] %s".formatted(name, arg))
				    .setStyle(Style.EMPTY.withColor(color))
		));
	}
}
