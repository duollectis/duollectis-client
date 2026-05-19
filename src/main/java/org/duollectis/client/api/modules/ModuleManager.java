package org.duollectis.client.api.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import org.duollectis.client.utils.JsonHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

	public void add(Module module) {
		ModuleInfo info = module.getClass().getAnnotation(ModuleInfo.class);
		module.setId(info.value());

		modules.add(module);
		instanceMap.put(module.getClass(), module);
	}

	public void disableAll() {
		modules.forEach(Module::disable);
	}

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

	public void load() {
		File file = resolveConfigFile();

		if (file.exists() == false) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			if (root.has("modules") == false) {
				return;
			}

			disableAll();

			JsonObject modulesTag = root.getAsJsonObject("modules");

			for (Module module : modules) {
				if (modulesTag.has(module.getId()) == false) {
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

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> clazz) {
		return (T) instanceMap.get(clazz);
	}

	public Module get(String name) {
		return modules.stream()
		              .filter(m -> m.getId().equalsIgnoreCase(name))
		              .findFirst()
		              .orElse(null);
	}

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
