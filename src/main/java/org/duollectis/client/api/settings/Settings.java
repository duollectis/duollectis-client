package org.duollectis.client.api.settings;

import com.google.gson.JsonObject;
import org.duollectis.client.settings.SettingGroup;
import org.duollectis.client.utils.ITranslatable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Settings {

	private final List<Setting<?>> settings = new ArrayList<>();

	private final ITranslatable parent;

	public Settings(ITranslatable parent) {
		this.parent = parent;
	}

	public <T extends Setting<?>> T add(T setting) {
		setting.parent = parent;
		settings.add(setting);
		return setting;
	}

	public void reset() {
		for (Setting<?> setting : settings) {
			setting.reset();
		}
	}

	public List<Setting<?>> all() {
		return List.copyOf(settings);
	}

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

	public void write(JsonObject target) {
		for (Setting<?> setting : settings) {
			if (setting.wasChanged()) {
				target.add(setting.getId(), setting.writeValue());
			}
		}
	}

	public void read(JsonObject source) {
		reset();

		for (Setting<?> setting : settings) {
			if (source.has(setting.getId())) {
				setting.readValue(source.get(setting.getId()));
			}
		}
	}
}
