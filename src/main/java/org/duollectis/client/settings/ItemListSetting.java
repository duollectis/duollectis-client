package org.duollectis.client.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.utils.IVisible;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Настройка списка предметов Minecraft. Поддерживает опциональный фильтр,
 * ограничивающий допустимые предметы, и флаг обхода фильтра при сохранении/загрузке
 * (нужен для корректного восстановления ранее сохранённых значений).
 */
public class ItemListSetting extends Setting<List<Item>> {

	@Getter
	private final Predicate<Item> filter;

	private final boolean bypassFilterWhenSavingAndLoading;

	private ItemListSetting(
			String id,
			String group,
			List<Item> defaultValue,
			Consumer<List<Item>> onChanged,
			Consumer<Setting<List<Item>>> onModuleActivated,
			IVisible visible,
			Predicate<Item> filter,
			boolean bypassFilterWhenSavingAndLoading
	) {
		super(id, group, defaultValue, onChanged, onModuleActivated, visible);

		this.filter = filter;
		this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
	}

	@Override
	protected List<Item> parseImpl(String str) {
		String[] parts = str.split(",");
		List<Item> items = new ArrayList<>(parts.length);

		for (String part : parts) {
			Item item = parseId(Registries.ITEM, part);

			if (item == null) {
				continue;
			}

			if (filter == null || filter.test(item)) {
				items.add(item);
			}
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
	public JsonElement writeValue() {
		JsonArray array = new JsonArray();

		for (Item item : get()) {
			if (bypassFilterWhenSavingAndLoading || filter == null || filter.test(item)) {
				array.add(Registries.ITEM.getId(item).toString());
			}
		}

		return array;
	}

	@Override
	public void readValue(JsonElement element) {
		get().clear();

		for (JsonElement entry : element.getAsJsonArray()) {
			Item item = Registries.ITEM.get(Identifier.of(entry.getAsString()));

			if (bypassFilterWhenSavingAndLoading || filter == null || filter.test(item)) {
				get().add(item);
			}
		}
	}

	public static Builder create(String id) {
		return new Builder(id);
	}

	public static class Builder extends SettingBuilder<Builder, List<Item>, ItemListSetting> {

		private Predicate<Item> filter;
		private boolean bypassFilterWhenSavingAndLoading = false;

		private Builder(String id) {
			super(id, new ArrayList<>(0));
		}

		public Builder filter(Predicate<Item> filter) {
			this.filter = filter;
			return this;
		}

		public Builder bypassFilter() {
			this.bypassFilterWhenSavingAndLoading = true;
			return this;
		}

		@Override
		public ItemListSetting done() {
			return new ItemListSetting(
					id,
					group,
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
