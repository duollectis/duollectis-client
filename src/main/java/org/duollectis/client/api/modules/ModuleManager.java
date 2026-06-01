package org.duollectis.client.api.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.duollectis.client.api.bind.BindManager;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.GameCloseEvent;
import org.duollectis.client.utils.JsonHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.duollectis.client.utils.MC.gameDirFile;

/**
 * Синглтон-менеджер всех модулей клиента.
 * Загружает классы модулей из реестра {@code META-INF/duollectis-modules},
 * сохраняет и восстанавливает их состояние через JSON-конфиг.
 */
@Slf4j
public final class ModuleManager {

	@Getter
	private static final ModuleManager instance = new ModuleManager();

	private static final String MODULE_REGISTRY = "META-INF/duollectis-modules";

	private final List<Module> modules = new ArrayList<>();
	private final Map<Class<? extends Module>, Module> instanceMap = new Reference2ReferenceOpenHashMap<>();

	private boolean init;

	private ModuleManager() {}

	public void init() {
		if (init) {
			throw new RuntimeException("Double initialization!");
		}

		EventBus.getInstance().subscribe(GameCloseEvent.class, event -> save());
		loadModulesFromRegistry();
		BindManager.getInstance().load();
		load();

		init = true;
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
		BindManager.getInstance().registerModule(info.value(), module::toggle);
		module.onInit();
	}

	/**
	 * Выключает все зарегистрированные модули.
	 */
	public void disableAll() {
		modules.forEach(Module::disable);
	}

	/**
	 * Сохраняет состояние всех модулей (включено/выключено + настройки) в JSON-файл конфига.
	 * Файл располагается в директории {@code <minecraft>/duollectis/settings.json}.
	 */
	public void save() {
		log.info("Сохранение модулей...");
		File file = resolveConfigFile();
		JsonObject modulesTag = new JsonObject();

		for (Module module : modules) {
			modulesTag.add(module.getId(), module.toJson());
		}

		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			JsonHelper.GSON.toJson(modulesTag, writer);
		}
		catch (IOException e) {
			log.error("Не удалось сохранить настройки модулей", e);
		}

		BindManager.getInstance().save();
	}

	/**
	 * Загружает модули из реестра и восстанавливает их состояние из JSON-конфига.
	 * Если конфиг отсутствует, модули остаются в состоянии по умолчанию.
	 */
	public void load() {
		log.info("Загрузка модулей...");
		File file = resolveConfigFile();

		if (!file.exists()) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			JsonObject modulesTag = JsonParser.parseReader(reader).getAsJsonObject();

			disableAll();

			for (Module module : modules) {

				if (modulesTag.has(module.getId())) {
					try {
						module.fromJson(modulesTag.getAsJsonObject(module.getId()));
					}
					catch (Exception e) {
						log.error("Не удалось загрузить {}", module.getId());
						log.error("", e);
					}
				}
				else {
					log.warn("Модуль не найден {}", module.getId());
				}
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
		return new File(gameDirFile(), "duollectis/settings.json");
	}

}
