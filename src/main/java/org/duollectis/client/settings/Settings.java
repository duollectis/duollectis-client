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

public class Settings implements ISerializable<Settings>, Iterable<SettingGroup> {

	private SettingGroup defaultGroup;
	private boolean invalidate;

	public final List<SettingGroup> groups = new ArrayList<>(1);

	public void reset() {
		for (SettingGroup group : groups) {
			for (Setting<?> setting : group) {
				setting.reset();
			}
		}

		invalidate();
	}

	public void invalidate() {
		invalidate = true;
	}

	public SettingGroup getGroup(String name) {
		for (SettingGroup sg : this) {
			if (sg.name.equals(name))
				return sg;
		}

		return null;
	}

	public SettingGroup getDefaultGroup() {
		if (defaultGroup == null)
			defaultGroup = createGroup("General");
		return defaultGroup;
	}

	public SettingGroup createGroup(String name, boolean expanded) {
		SettingGroup group = new SettingGroup(name, expanded);
		groups.add(group);
		return group;
	}

	public SettingGroup createGroup(String name) {
		return createGroup(name, true);
	}

	@Override
	public @NotNull Iterator<SettingGroup> iterator() {
		return groups.iterator();
	}

	@Override
	public JsonObject toTag() {
		JsonObject tag = new JsonObject();

		JsonArray groupsTag = new JsonArray();

		for (SettingGroup group : groups) {
			if (group.wasChanged())
				groupsTag.add(group.toTag());
		}

		if (!groupsTag.isEmpty())
			tag.add("groups", groupsTag);

		return tag;
	}

	@Override
	public Settings fromTag(JsonObject tag) {
		reset();

		JsonArray groupsTag = tag.has("groups") ? tag.get("groups").getAsJsonArray() : new JsonArray();

		for (JsonElement t : groupsTag) {
			JsonObject groupTag = t.getAsJsonObject();

			SettingGroup sg = getGroup(groupTag.get("name").getAsString());
			if (sg != null)
				sg.fromTag(groupTag);
		}

		return this;
	}
}
