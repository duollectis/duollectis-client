package org.duollectis.client.api.bind;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.duollectis.client.utils.JsonHelper;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.duollectis.client.utils.MC.gameDir;
import static org.duollectis.client.utils.MC.mc;

/**
 * Централизованный менеджер всех клавишных биндов клиента.
 * Хранит бинды модулей (по id модуля) и системные бинды (открытие меню, ClickGui).
 * Нажатия клавиш поступают через {@link #onKey(int)} из {@code KeyboardMixin} — без polling.
 * Системные бинды представлены объектами {@link KeyBinding}, которые инжектируются
 * в {@code GameOptions.allKeys} через миксин и отображаются в экране настроек управления MC.
 * Сохраняет/загружает бинды в {@code duollectis/binds.json}.
 */
@Slf4j
public final class BindManager {

	private static final int NO_KEY = -1;
	private static final String BINDS_FILE = "duollectis/binds.json";
	private static final String KEY_MODULES = "modules";
	private static final String KEY_SYSTEM = "system";

	public static final String BIND_MENU = "duollectis.key.menu";
	public static final String BIND_CLICKGUI = "duollectis.key.clickgui";

	/** Категория биндов клиента в экране настроек управления MC. */
	public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
		Identifier.of("duollectis", "client")
	);

	/** KeyBinding для открытия главного меню — виден в настройках управления MC. */
	public static final KeyBinding KEY_MENU = new KeyBinding(
		BIND_MENU,
		InputUtil.Type.KEYSYM,
		GLFW.GLFW_KEY_RIGHT_SHIFT,
		CATEGORY
	);

	/** KeyBinding для открытия ClickGui — виден в настройках управления MC. */
	public static final KeyBinding KEY_CLICKGUI = new KeyBinding(
		BIND_CLICKGUI,
		InputUtil.Type.KEYSYM,
		GLFW.GLFW_KEY_RIGHT_CONTROL,
		CATEGORY
	);

	@Getter
	private static final BindManager instance = new BindManager();

	/** Бинды модулей: ключ — id модуля, значение — keyCode GLFW. */
	private final Map<String, Integer> moduleBind = new LinkedHashMap<>();

	/** Действия системных биндов: ключ — id бинда, значение — действие при нажатии. */
	private final Map<String, Runnable> systemActions = new LinkedHashMap<>();

	/** Действия модульных биндов: ключ — id модуля, значение — действие при нажатии. */
	private final Map<String, Runnable> moduleActions = new LinkedHashMap<>();

	private BindManager() {}

	/**
	 * Регистрирует системный бинд с действием.
	 * Вызывается при инициализации клиента для меню и ClickGui.
	 *
	 * @param bindId   идентификатор бинда (одна из констант {@link #BIND_MENU}, {@link #BIND_CLICKGUI})
	 * @param action   действие при нажатии клавиши
	 */
	public void registerSystemAction(String bindId, Runnable action) {
		systemActions.put(bindId, action);
	}

	/**
	 * Регистрирует слот бинда для модуля с действием.
	 * Если бинд уже загружен с диска — он сохраняется.
	 *
	 * @param moduleId идентификатор модуля
	 * @param action   действие при нажатии клавиши (обычно {@code module::toggle})
	 */
	public void registerModule(String moduleId, Runnable action) {
		moduleBind.putIfAbsent(moduleId, NO_KEY);
		moduleActions.put(moduleId, action);
	}

	public int getModuleKey(String moduleId) {
		return moduleBind.getOrDefault(moduleId, NO_KEY);
	}

	public void setModuleKey(String moduleId, int keyCode) {
		moduleBind.put(moduleId, keyCode);
	}

	public void clearModuleKey(String moduleId) {
		moduleBind.put(moduleId, NO_KEY);
	}

	/**
	 * Возвращает актуальный keyCode системного бинда, читая его напрямую из {@link KeyBinding#boundKey}.
	 * Это гарантирует синхронизацию с изменениями, сделанными пользователем в экране управления MC.
	 */
	public int getSystemKey(String bindId) {
		if (BIND_MENU.equals(bindId)) {
			return KEY_MENU.boundKey.getCode();
		}

		if (BIND_CLICKGUI.equals(bindId)) {
			return KEY_CLICKGUI.boundKey.getCode();
		}

		return NO_KEY;
	}

	/**
	 * Возвращает отображаемое имя клавиши бинда модуля, или {@code null} если бинд не задан.
	 *
	 * @param moduleId идентификатор модуля
	 */
	public String getModuleKeyName(String moduleId) {
		int code = getModuleKey(moduleId);

		return code == NO_KEY
			? null
			: InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
	}

	/**
	 * Обрабатывает нажатие клавиши. Вызывается из {@code KeyboardMixin} при GLFW_PRESS.
	 * Системные бинды срабатывают всегда, модульные — только когда нет открытого экрана.
	 * Коды системных биндов читаются из {@link KeyBinding#boundKey} — автосинхронизация
	 * с настройками управления MC без дополнительных усилий.
	 *
	 * @param key keyCode GLFW нажатой клавиши
	 */
	public void onKey(int key) {
		if (key == KEY_MENU.boundKey.getCode()) {
			Runnable action = systemActions.get(BIND_MENU);

			if (action != null) {
				action.run();
			}

			return;
		}

		if (key == KEY_CLICKGUI.boundKey.getCode()) {
			Runnable action = systemActions.get(BIND_CLICKGUI);

			if (action != null) {
				action.run();
			}

			return;
		}

		if (mc.currentScreen != null) {
			return;
		}

		for (Map.Entry<String, Integer> entry : moduleBind.entrySet()) {
			if (entry.getValue() != key) {
				continue;
			}

			Runnable action = moduleActions.get(entry.getKey());

			if (action != null) {
				action.run();
			}
		}
	}

	/**
	 * Сохраняет бинды модулей и системных клавиш в {@code duollectis/binds.json}.
	 * Системные бинды сохраняются как коды из {@link KeyBinding#boundKey} для
	 * корректного восстановления после изменений в экране управления MC.
	 */
	public void save() {
		File file = resolveFile();
		file.getParentFile().mkdirs();

		JsonObject modulesTag = new JsonObject();

		for (Map.Entry<String, Integer> entry : moduleBind.entrySet()) {
			modulesTag.addProperty(entry.getKey(), entry.getValue());
		}

		JsonObject systemTag = new JsonObject();
		systemTag.addProperty(BIND_MENU, KEY_MENU.boundKey.getCode());
		systemTag.addProperty(BIND_CLICKGUI, KEY_CLICKGUI.boundKey.getCode());

		JsonObject root = new JsonObject();
		root.add(KEY_MODULES, modulesTag);
		root.add(KEY_SYSTEM, systemTag);

		try (FileWriter writer = new FileWriter(file)) {
			JsonHelper.GSON.toJson(root, writer);
		}
		catch (IOException e) {
			log.error("Не удалось сохранить бинды", e);
		}
	}

	/**
	 * Загружает бинды из {@code duollectis/binds.json}.
	 * Восстанавливает {@link KeyBinding#boundKey} для системных биндов, чтобы
	 * экран управления MC отображал сохранённые клавиши.
	 * Вызывается после регистрации всех модулей.
	 */
	public void load() {
		File file = resolveFile();

		if (!file.exists()) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			if (root.has(KEY_MODULES)) {
				JsonObject modulesTag = root.getAsJsonObject(KEY_MODULES);

				for (String id : modulesTag.keySet()) {
					moduleBind.put(id, modulesTag.get(id).getAsInt());
				}
			}

			if (root.has(KEY_SYSTEM)) {
				JsonObject systemTag = root.getAsJsonObject(KEY_SYSTEM);

				if (systemTag.has(BIND_MENU)) {
					int code = systemTag.get(BIND_MENU).getAsInt();
					KEY_MENU.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(code));
				}

				if (systemTag.has(BIND_CLICKGUI)) {
					int code = systemTag.get(BIND_CLICKGUI).getAsInt();
					KEY_CLICKGUI.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(code));
				}

				KeyBinding.updateKeysByCode();
			}
		}
		catch (Exception e) {
			log.error("Не удалось загрузить бинды", e);
		}
	}

	private static File resolveFile() {
		return gameDir().resolve(BINDS_FILE).toFile();
	}
}
