/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.duollectis.client.settings;

import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtCompound;
import org.duollectis.client.utils.IVisible;

import java.util.List;
import java.util.function.Consumer;

public class BoolSetting extends Setting<Boolean> {

	private static final List<String> SUGGESTIONS = List.of("true", "false", "toggle");

	private BoolSetting(
			String name,
			String description,
			Boolean defaultValue,
			Consumer<Boolean> onChanged,
			Consumer<Setting<Boolean>> onModuleActivated,
			IVisible visible
	) {
		super(name, description, defaultValue, onChanged, onModuleActivated, visible);
	}

	@Override
	protected Boolean parseImpl(String str) {
		if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("1"))
			return true;
		else if (str.equalsIgnoreCase("false") || str.equalsIgnoreCase("0"))
			return false;
		else if (str.equalsIgnoreCase("toggle"))
			return !get();
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
	public JsonObject save(JsonObject tag) {
		tag.addProperty("value", get());
		return tag;
	}

	@Override
	protected Boolean load(JsonObject tag) {
		set(tag.has("value") && tag.get("value").getAsBoolean());
		return get();
	}

	public static class Builder extends SettingBuilder<Builder, Boolean, BoolSetting> {

		public Builder() {
			super(false);
		}

		public BoolSetting build() {
			return new BoolSetting(name, description, defaultValue, onChanged, onModuleActivated, visible);
		}
	}
}
