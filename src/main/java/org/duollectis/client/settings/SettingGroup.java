/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.duollectis.client.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.duollectis.client.utils.ISerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingGroup implements ISerializable<SettingGroup>, Iterable<Setting<?>> {

	public final String name;
	public boolean sectionExpanded;

	final List<Setting<?>> settings = new ArrayList<>(1);

	SettingGroup(String name, boolean sectionExpanded) {
		this.name = name;
		this.sectionExpanded = sectionExpanded;
	}

	public Setting<?> get(String name) {
		for (Setting<?> setting : this) {
			if (setting.name.equals(name))
				return setting;
		}

		return null;
	}

	public <T extends Setting<?>> T add(T setting) {
		settings.add(setting);

		return setting;
	}

	public boolean wasChanged() {
		for (Setting<?> setting : settings) {
			if (setting.wasChanged())
				return true;
		}
		return false;
	}

	@Override
	public @NotNull Iterator<Setting<?>> iterator() {
		return settings.iterator();
	}

	@Override
	public JsonObject toTag() {
		JsonObject tag = new JsonObject();

		tag.addProperty("name", name);
		tag.addProperty("sectionExpanded", sectionExpanded);

		JsonArray settingsTag = new JsonArray();

		for (Setting<?> setting : this) {
			if (setting.wasChanged())
				settingsTag.add(setting.toTag());
		}

		if (!settingsTag.isEmpty())
			tag.add("settings", settingsTag);

		return tag;
	}

	@Override
	public SettingGroup fromTag(JsonObject tag) {
		sectionExpanded = tag.has("sectionExpanded") && tag.get("sectionExpanded").getAsBoolean();

		JsonArray settingsTag = tag.has("settings") ? tag.getAsJsonArray("settings") : new JsonArray();

		for (JsonElement t : settingsTag) {
			JsonObject object = t.getAsJsonObject();

			Setting<?> setting = get(object.get("name").getAsString());
			if (setting != null) {
				setting.fromTag(object);
			}
		}

		return this;
	}
}
