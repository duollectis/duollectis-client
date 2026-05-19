package org.duollectis.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.utils.IVisible;

import java.util.function.Consumer;

public class IntSetting extends Setting<Integer> {

	@Getter
	private final int min;

	@Getter
	private final int max;

	@Getter
	private final int sliderMin;

	@Getter
	private final int sliderMax;

	@Getter
	private final boolean noSlider;

	private IntSetting(
			String id,
			String group,
			int defaultValue,
			Consumer<Integer> onChanged,
			Consumer<Setting<Integer>> onModuleActivated,
			IVisible visible,
			int min,
			int max,
			int sliderMin,
			int sliderMax,
			boolean noSlider
	) {
		super(id, group, defaultValue, onChanged, onModuleActivated, visible);

		this.min = min;
		this.max = max;
		this.sliderMin = sliderMin;
		this.sliderMax = sliderMax;
		this.noSlider = noSlider;
	}

	@Override
	protected Integer parseImpl(String str) {
		try {
			return Integer.parseInt(str.trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	@Override
	protected boolean isValueValid(Integer value) {
		return value >= min && value <= max;
	}

	@Override
	public JsonElement writeValue() {
		return new JsonPrimitive(get());
	}

	@Override
	public void readValue(JsonElement element) {
		set(element.getAsInt());
	}

	public static Builder create(String id) {
		return new Builder(id);
	}

	public static class Builder extends SettingBuilder<Builder, Integer, IntSetting> {

		private int min = Integer.MIN_VALUE;
		private int max = Integer.MAX_VALUE;
		private int sliderMin = 0;
		private int sliderMax = 10;
		private boolean noSlider = false;

		private Builder(String id) {
			super(id, 0);
		}

		public Builder defaultValue(int defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		public Builder min(int min) {
			this.min = min;
			return this;
		}

		public Builder range(int min, int max) {
			this.min = Math.min(min, max);
			this.max = Math.max(min, max);
			return this;
		}

		public Builder sliderRange(int min, int max) {
			this.sliderMin = min;
			this.sliderMax = max;
			return this;
		}

		public Builder noSlider() {
			this.noSlider = true;
			return this;
		}

		@Override
		public IntSetting done() {
			return new IntSetting(
					id,
					group,
					defaultValue,
					onChanged,
					onModuleActivated,
					visible,
					min,
					max,
					Math.max(sliderMin, min),
					Math.min(sliderMax, max),
					noSlider
			);
		}

	}

}
