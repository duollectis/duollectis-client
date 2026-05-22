package org.duollectis.client.api.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import org.duollectis.client.utils.JsonHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Синглтон-менеджер всех модулей клиента.
 * Загружает классы модулей из реестра {@code META-INF/duollectis-modules},
 * сохраняет и восстанавливает их состояние через JSON-конфиг.
 */
@Slf4j
public final class ModuleManager {

	private static final String MODULE_REGISTRY = "META-INF/duollectis-modules";

	@Getter
	private static final ModuleManager instance = new ModuleManager();

	private final List<Module> modules = new ArrayList<>();
	private final Map<Class<? extends Module>, Module> instanceMap = new Reference2ReferenceOpenHashMap<>();

	private ModuleManager() {
		loadModulesFromRegistry();
		load();
	}

	/**
	 * Регистрирует модуль в менеджере и устанавливает ему идентификатор из {@link ModuleInfo}.
	 *
	 * @param module экземпляр модуля
	 */
	public void add(Module module) {
		ModuleInfo info = module.getClass().getAnnotation(ModuleInfo.class);
		module.setId(info.value());

		modules.add(module);
		instanceMap.put(module.getClass(), module);
	}

	/** Выключает все зарегистрированные модули. */
	public void disableAll() {
		modules.forEach(Module::disable);
	}

	/**
	 * Сохраняет состояние всех модулей (включено/выключено + настройки) в JSON-файл конфига.
	 * Файл располагается в директории {@code <minecraft>/duollectis/settings.json}.
	 */
	public void save() {
		File file = resolveConfigFile();
		JsonObject root = new JsonObject();
		JsonObject modulesTag = new JsonObject();

		for (Module module : modules) {
			modulesTag.add(module.getId(), module.toJson());
		}

		root.add("modules", modulesTag);
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			JsonHelper.GSON.toJson(root, writer);
		}
		catch (IOException e) {
			log.error("Не удалось сохранить настройки модулей", e);
		}
	}

	/**
	 * Загружает модули из реестра и восстанавливает их состояние из JSON-конфига.
	 * Если конфиг отсутствует, модули остаются в состоянии по умолчанию.
	 */
	public void load() {
		File file = resolveConfigFile();

		if (!file.exists()) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			if (!root.has("modules")) {
				return;
			}

			disableAll();

			JsonObject modulesTag = root.getAsJsonObject("modules");

			for (Module module : modules) {
				if (!modulesTag.has(module.getId())) {
					continue;
				}

				module.fromJson(modulesTag.getAsJsonObject(module.getId()));
			}
		}
		catch (Exception e) {
			log.error("Не удалось загрузить настройки модулей", e);
		}
	}

	public Collection<Module> all() {
		return List.copyOf(modules);
	}

	/**
	 * Возвращает модуль по его классу.
	 *
	 * @param clazz класс модуля
	 * @return экземпляр модуля или {@code null}, если не зарегистрирован
	 */
	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> clazz) {
		return (T) instanceMap.get(clazz);
	}

	/**
	 * Возвращает модуль по его строковому идентификатору (без учёта регистра).
	 *
	 * @param name идентификатор модуля (значение аннотации {@link ModuleInfo})
	 * @return экземпляр модуля или {@code null}, если не найден
	 */
	public Module get(String name) {
		return modules.stream()
		              .filter(m -> m.getId().equalsIgnoreCase(name))
		              .findFirst()
		              .orElse(null);
	}

	/**
	 * Читает файл реестра {@code META-INF/duollectis-modules} и инстанциирует каждый класс.
	 * Файл генерируется {@link ModuleRegistryProcessor} во время компиляции.
	 */
	private void loadModulesFromRegistry() {
		InputStream stream = getClass().getClassLoader().getResourceAsStream(MODULE_REGISTRY);

		if (stream == null) {
			log.warn("Реестр модулей не найден: {}", MODULE_REGISTRY);
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;

			while ((line = reader.readLine()) != null) {
				String className = line.trim();

				if (className.isEmpty()) {
					continue;
				}

				instantiateModule(className);
			}
		}
		catch (IOException e) {
			log.error("Не удалось прочитать реестр модулей", e);
		}
	}

	/**
	 * Создаёт экземпляр модуля по имени класса через рефлексию и регистрирует его.
	 *
	 * @param className полное имя класса модуля
	 */
	private void instantiateModule(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			Module module = (Module) clazz.getDeclaredConstructor().newInstance();
			add(module);
		}
		catch (Exception e) {
			log.error("Не удалось создать модуль: {}", className, e);
		}
	}

	private static File resolveConfigFile() {
		File minecraftDir = FabricLoader.getInstance().getGameDir().toFile();
		return new File(minecraftDir, "duollectis/settings.json");
	}

}
