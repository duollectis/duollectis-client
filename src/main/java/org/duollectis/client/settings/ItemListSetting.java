/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.duollectis.client.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.duollectis.client.utils.IVisible;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemListSetting extends Setting<List<Item>> {

	public final Predicate<Item> filter;
	private final boolean bypassFilterWhenSavingAndLoading;

	public ItemListSetting(
			String name,
			String description,
			List<Item> defaultValue,
			Consumer<List<Item>> onChanged,
			Consumer<Setting<List<Item>>> onModuleActivated,
			IVisible visible,
			Predicate<Item> filter,
			boolean bypassFilterWhenSavingAndLoading
	) {
		super(name, description, defaultValue, onChanged, onModuleActivated, visible);

		this.filter = filter;
		this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
	}

	@Override
	protected List<Item> parseImpl(String str) {
		String[] values = str.split(",");
		List<Item> items = new ArrayList<>(values.length);

		try {
			for (String value : values) {
				Item item = parseId(Registries.ITEM, value);
				if (item != null && (filter == null || filter.test(item)))
					items.add(item);
			}
		}
		catch (Exception ignored) {
		}

		return items;
	}

	@Override
	public void resetImpl() {
		value = new ArrayList<>(defaultValue);
	}

	@Override
	protected boolean isValueValid(List<Item> value) {
		return true;
	}

	@Override
	public Iterable<Identifier> getIdentifierSuggestions() {
		return Registries.ITEM.getIds();
	}

	@Override
	public JsonObject save(JsonObject tag) {
		JsonArray valueTag = new JsonArray();

		for (Item item : get()) {
			if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item))) {
				valueTag.add(Registries.ITEM.getId(item).toString());
			}
		}

		tag.add("value", valueTag);

		return tag;
	}

	@Override
	public List<Item> load(JsonObject tag) {
		get().clear();

		JsonArray valueTag = tag.has("value") ? tag.getAsJsonArray("value") : new JsonArray();

		for (JsonElement tagI : valueTag) {
			Item item = Registries.ITEM.get(Identifier.of(tagI.getAsString()));

			if (bypassFilterWhenSavingAndLoading || (filter == null || filter.test(item)))
				get().add(item);
		}

		return get();
	}

	public static class Builder extends SettingBuilder<Builder, List<Item>, ItemListSetting> {

		private Predicate<Item> filter;
		private boolean bypassFilterWhenSavingAndLoading;

		public Builder() {
			super(new ArrayList<>(0));
		}

		public ItemListSetting build() {
			return new ItemListSetting(
					name,
					description,
					defaultValue,
					onChanged,
					onModuleActivated,
					visible,
					filter,
					bypassFilterWhenSavingAndLoading
			);
		}
	}
}
