package org.duollectis.client.api.settings;

import com.google.gson.JsonElement;
import lombok.Getter;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.duollectis.client.utils.ITranslatable;
import org.duollectis.client.utils.IVisible;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Setting<T> implements ITranslatable {

	private static final String DEFAULT_GROUP = "General";
	private static final List<String> NO_SUGGESTIONS = new ArrayList<>(0);

	@Getter
	private final String id;

	@Getter
	private final String group;

	private final IVisible visible;

	@Getter
	protected final T defaultValue;

	protected T value;

	public final Consumer<Setting<T>> onModuleActivated;

	private final Consumer<T> onChanged;

	@Nullable
	ITranslatable parent;

	public Setting(
			String id,
			String group,
			T defaultValue,
			Consumer<T> onChanged,
			Consumer<Setting<T>> onModuleActivated,
			IVisible visible
	) {
		this.id = id;
		this.group = group;
		this.defaultValue = defaultValue;
		this.onChanged = onChanged;
		this.onModuleActivated = onModuleActivated;
		this.visible = visible;

		resetImpl();
	}

	@Override
	public String getTranslationPath() {
		return (parent == null
		       ? "duollectis.client.global.settings."
		       : parent.getTranslationPath() + ".settings.") + id;
	}

	public T get() {
		return value;
	}

	public boolean set(T value) {
		if (isValueValid(value) == false) {
			return false;
		}

		this.value = value;
		onChanged();

		return true;
	}

	protected void resetImpl() {
		value = defaultValue;
	}

	public void reset() {
		resetImpl();
		onChanged();
	}

	public boolean parse(String str) {
		T newValue = parseImpl(str);

		if (newValue == null) {
			return false;
		}

		if (isValueValid(newValue)) {
			value = newValue;
			onChanged();
		}

		return true;
	}

	public boolean wasChanged() {
		return Objects.equals(value, defaultValue) == false;
	}

	public void onChanged() {
		if (onChanged == null) {
			return;
		}

		onChanged.accept(value);
	}

	public void onActivated() {
		if (onModuleActivated == null) {
			return;
		}

		onModuleActivated.accept(this);
	}

	public boolean isVisible() {
		if (visible == null) {
			return true;
		}

		return visible.isVisible();
	}

	protected abstract T parseImpl(String str);

	protected abstract boolean isValueValid(T value);

	public Iterable<Identifier> getIdentifierSuggestions() {
		return null;
	}

	public List<String> getSuggestions() {
		return NO_SUGGESTIONS;
	}

	public abstract JsonElement writeValue();

	public abstract void readValue(JsonElement element);

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Setting<?> setting = (Setting<?>) o;

		return Objects.equals(id, setting.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Nullable
	public static <T> T parseId(Registry<T> registry, String name) {
		String trimmed = name.trim();

		Identifier identifier = trimmed.contains(":")
		                        ? Identifier.of(trimmed)
		                        : Identifier.of("minecraft", trimmed);

		if (registry.containsId(identifier)) {
			return registry.get(identifier);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public abstract static class SettingBuilder<B, V, S extends Setting<V>> {

		protected final String id;
		protected String group = DEFAULT_GROUP;
		protected V defaultValue;
		protected IVisible visible;
		protected Consumer<V> onChanged;
		protected Consumer<Setting<V>> onModuleActivated;

		protected SettingBuilder(String id, V defaultValue) {
			this.id = id;
			this.defaultValue = defaultValue;
		}

		public B group(String group) {
			this.group = group;
			return (B) this;
		}

		public B defaultValue(V defaultValue) {
			this.defaultValue = defaultValue;
			return (B) this;
		}

		public B visible(IVisible visible) {
			this.visible = visible;
			return (B) this;
		}

		public B onChanged(Consumer<V> onChanged) {
			this.onChanged = onChanged;
			return (B) this;
		}

		public B onModuleActivated(Consumer<Setting<V>> onModuleActivated) {
			this.onModuleActivated = onModuleActivated;
			return (B) this;
		}

		public abstract S done();

	}

}
