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

/**
 * Абстрактный базовый класс для всех настроек модуля.
 * Хранит текущее значение, значение по умолчанию, ограничения видимости
 * и колбэки на изменение. Реализует {@link ITranslatable} для локализации названия.
 * Конкретные типы создаются через вложенный {@link SettingBuilder}.
 */
public abstract class Setting<T> implements ITranslatable {

	private static final String DEFAULT_GROUP = "General";
	private static final List<String> NO_SUGGESTIONS = new ArrayList<>(0);

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
	public String getId() {
		return id;
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

	/**
	 * Устанавливает новое значение настройки с валидацией.
	 * Если значение изменилось, вызывает зарегистрированный колбэк {@code onChanged}.
	 *
	 * @param newValue новое значение
	 * @return {@code true}, если значение прошло валидацию и было применено
	 */
	public boolean set(T newValue) {
		if (!isValueValid(newValue)) {
			return false;
		}

		value = newValue;
		onChanged();

		return true;
	}

	protected void resetImpl() {
		value = defaultValue;
	}

	/** Сбрасывает значение настройки к значению по умолчанию. */
	public void reset() {
		resetImpl();
		onChanged();
	}

	/**
	 * Парсит строковое представление значения и применяет его.
	 * Используется при вводе значения вручную в текстовое поле GUI.
	 *
	 * @param str строковое представление нового значения
	 * @return {@code true}, если строка успешно распарсена и значение применено
	 */
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

	/** @return {@code true}, если текущее значение отличается от значения по умолчанию */
	public boolean wasChanged() {
		return !Objects.equals(value, defaultValue);
	}

	/**
	 * Вызывается при каждом изменении значения настройки.
	 * Делегирует в зарегистрированный колбэк {@code onChanged}, если он задан.
	 */
	public void onChanged() {
		if (onChanged == null) {
			return;
		}

		onChanged.accept(value);
	}

	/**
	 * Вызывается при включении родительского модуля.
	 * Используется для настроек, которые должны реагировать на активацию модуля.
	 */
	public void onActivated() {
		if (onModuleActivated == null) {
			return;
		}

		onModuleActivated.accept(this);
	}

	public boolean isVisible() {
		return visible == null || visible.isVisible();
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

	/**
	 * Ищет объект в реестре Minecraft по строковому идентификатору.
	 * Если строка не содержит {@code ":"}, автоматически добавляет пространство имён {@code minecraft:}.
	 *
	 * @param registry реестр для поиска
	 * @param name     строковый идентификатор (с пространством имён или без)
	 * @return найденный объект или {@code null}, если не найден
	 */
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

	/**
	 * Базовый строитель для создания настроек через fluent API.
	 * Подклассы добавляют специфичные для типа параметры (диапазон, суффикс и т.д.).
	 *
	 * @param <B> тип конкретного строителя (для цепочки вызовов)
	 * @param <V> тип значения настройки
	 * @param <S> тип создаваемой настройки
	 */
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
