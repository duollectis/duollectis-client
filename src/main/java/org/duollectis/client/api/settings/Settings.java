package org.duollectis.client.api.settings;

import com.google.gson.JsonObject;
import org.duollectis.client.settings.SettingGroup;
import org.duollectis.client.utils.ITranslatable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Контейнер настроек модуля.
 * Хранит упорядоченный список настроек, группирует их по группам
 * и обеспечивает сериализацию/десериализацию в JSON.
 */
public class Settings {

	private final List<Setting<?>> settings = new ArrayList<>();

	private final ITranslatable parent;

	public Settings(ITranslatable parent) {
		this.parent = parent;
	}

	/**
	 * Регистрирует настройку в контейнере и привязывает к родительскому объекту.
	 *
	 * @param setting настройка для регистрации
	 * @return та же настройка (для fluent-инициализации полей)
	 */
	public <T extends Setting<?>> T add(T setting) {
		setting.parent = parent;
		settings.add(setting);
		return setting;
	}

	/** Сбрасывает все настройки к значениям по умолчанию. */
	public void reset() {
		for (Setting<?> setting : settings) {
			setting.reset();
		}
	}

	/** @return неизменяемая копия списка всех настроек */
	public List<Setting<?>> all() {
		return List.copyOf(settings);
	}

	/**
	 * Группирует настройки по полю {@code group} и возвращает список групп.
	 * Порядок групп соответствует порядку первого появления каждой группы.
	 *
	 * @return список групп настроек
	 */
	public List<SettingGroup> getGroups() {
		Map<String, List<Setting<?>>> grouped = new LinkedHashMap<>();

		for (Setting<?> setting : settings) {
			grouped
					.computeIfAbsent(setting.getGroup(), k -> new ArrayList<>())
					.add(setting);
		}

		List<SettingGroup> result = new ArrayList<>(grouped.size());

		for (Map.Entry<String, List<Setting<?>>> entry : grouped.entrySet()) {
			result.add(new SettingGroup(entry.getKey(), List.copyOf(entry.getValue())));
		}

		return result;
	}

	/**
	 * Сериализует только изменённые настройки в переданный JSON-объект.
	 * Настройки со значением по умолчанию не записываются, чтобы не раздувать конфиг.
	 *
	 * @param target целевой JSON-объект
	 */
	public void write(JsonObject target) {
		for (Setting<?> setting : settings) {
			if (setting.wasChanged()) {
				target.add(setting.getId(), setting.writeValue());
			}
		}
	}

	/**
	 * Сбрасывает все настройки к умолчаниям, затем восстанавливает значения из JSON.
	 * Настройки, отсутствующие в JSON, остаются со значением по умолчанию.
	 *
	 * @param source исходный JSON-объект
	 */
	public void read(JsonObject source) {
		reset();

		for (Setting<?> setting : settings) {
			if (source.has(setting.getId())) {
				setting.readValue(source.get(setting.getId()));
			}
		}
	}
}
