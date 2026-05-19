package org.duollectis.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.utils.IVisible;

import java.util.List;
import java.util.function.Consumer;

public class BoolSetting extends Setting<Boolean> {

	private static final List<String> SUGGESTIONS = List.of("true", "false", "toggle");

	private BoolSetting(
			String id,
			String group,
			Boolean defaultValue,
			Consumer<Boolean> onChanged,
			Consumer<Setting<Boolean>> onModuleActivated,
			IVisible visible
	) {
		super(id, group, defaultValue, onChanged, onModuleActivated, visible);
	}

	@Override
	protected Boolean parseImpl(String str) {
		if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("1")) {
			return true;
		}

		if (str.equalsIgnoreCase("false") || str.equalsIgnoreCase("0")) {
			return false;
		}

		if (str.equalsIgnoreCase("toggle")) {
			return get() == false;
		}

		return null;
	}

	@Override
	protected boolean isValueValid(Boolean value) {
		return true;
	}

	@Override
	public List<String> getSuggestions() {
		return SUGGESTIONS;
	}

	@Override
	public JsonElement writeValue() {
		return new JsonPrimitive(get());
	}

	@Override
	public void readValue(JsonElement element) {
		set(element.getAsBoolean());
	}

	public static Builder create(String id) {
		return new Builder(id);
	}

	public static class Builder extends SettingBuilder<Builder, Boolean, BoolSetting> {

		private Builder(String id) {
			super(id, false);
		}

		@Override
		public BoolSetting done() {
			return new BoolSetting(
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
