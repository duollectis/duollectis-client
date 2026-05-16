package org.duollectis.client.settings;

import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtCompound;
import org.duollectis.client.utils.IVisible;

import java.awt.*;
import java.util.function.Consumer;

public class ColorSetting extends Setting<Color> {

	public ColorSetting(
			String name,
			String description,
			Color defaultValue,
			Consumer<Color> onChanged,
			Consumer<Setting<Color>> onModuleActivated,
			IVisible visible
	) {
		super(name, description, defaultValue, onChanged, onModuleActivated, visible);
	}

	@Override
	protected Color parseImpl(String str) {
		try {
			return new Color(Integer.parseUnsignedInt(str, 16), true);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	protected boolean isValueValid(Color value) {
		return value != null;
	}

	@Override
	protected JsonObject save(JsonObject tag) {
		// Сохраняем цвет как единое RGBA число типа int
		tag.addProperty("value", value.getRGB());
		return tag;
	}

	@Override
	protected Color load(JsonObject tag) {
		if (tag.has("value")) {
			return new Color(tag.get("value").getAsInt(), true);
		}

		return defaultValue;
	}

	// Классический Билдер в стиле Метеора
	public static class Builder extends SettingBuilder<Builder, Color, ColorSetting> {

		public Builder() {
			super(new Color(255, 255, 255, 255)); // Белый по умолчанию
		}

		public ColorSetting build() {
			return new ColorSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
		}
	}
}