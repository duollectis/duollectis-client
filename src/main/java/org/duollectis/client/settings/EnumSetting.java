package org.duollectis.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.utils.IVisible;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {

	private final T[] values;
	private final List<String> suggestions;

	@SuppressWarnings("unchecked")
	private EnumSetting(
			String id,
			String group,
			T defaultValue,
			Consumer<T> onChanged,
			Consumer<Setting<T>> onModuleActivated,
			IVisible visible
	) {
		super(id, group, defaultValue, onChanged, onModuleActivated, visible);

		values = (T[]) defaultValue.getDeclaringClass().getEnumConstants();
		suggestions = new ArrayList<>(values.length);

		for (T value : values) {
			suggestions.add(value.toString());
		}
	}

	@Override
	protected T parseImpl(String str) {
		for (T possibleValue : values) {
			if (str.equalsIgnoreCase(possibleValue.toString())) {
				return possibleValue;
			}
		}

		return null;
	}

	@Override
	protected boolean isValueValid(T value) {
		return true;
	}

	@Override
	public List<String> getSuggestions() {
		return suggestions;
	}

	@Override
	public JsonElement writeValue() {
		return new JsonPrimitive(get().toString());
	}

	@Override
	public void readValue(JsonElement element) {
		parse(element.getAsString());
	}

	public static <T extends Enum<?>> Builder<T> create(String id) {
		return new Builder<>(id);
	}

	public static class Builder<T extends Enum<?>> extends SettingBuilder<Builder<T>, T, EnumSetting<T>> {

		private Builder(String id) {
			super(id, null);
		}

		@Override
		public EnumSetting<T> done() {
			return new EnumSetting<>(
					id,
					group,
					defaultValue,
					onChanged,
					onModuleActivated,
					visible
			);
		}

	}

}
