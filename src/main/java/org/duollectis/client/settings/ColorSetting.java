package org.duollectis.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.utils.IVisible;
import org.duollectis.client.utils.color.Color;

import java.util.function.Consumer;

public class ColorSetting extends Setting<Color> {

	private ColorSetting(
			String id,
			String group,
			Color defaultValue,
			Consumer<Color> onChanged,
			Consumer<Setting<Color>> onModuleActivated,
			IVisible visible
	) {
		super(id, group, defaultValue, onChanged, onModuleActivated, visible);
	}

	@Override
	protected Color parseImpl(String str) {
		try {
			return new Color((int) Long.parseLong(str.trim(), 16));
		} catch (NumberFormatException ignored) {
			return defaultValue;
		}
	}

	@Override
	protected boolean isValueValid(Color value) {
		return value != null;
	}

	@Override
	public JsonElement writeValue() {
		return new JsonPrimitive(value.getPacked());
	}

	@Override
	public void readValue(JsonElement element) {
		set(new Color(element.getAsInt()));
	}

	public static Builder create(String id) {
		return new Builder(id);
	}

	public static class Builder extends SettingBuilder<Builder, Color, ColorSetting> {

		private Builder(String id) {
			super(id, new Color(255, 255, 255, 255));
		}

		public Builder defaultValue(Color defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		@Override
		public ColorSetting done() {
			return new ColorSetting(
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
