package org.duollectis.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.utils.IVisible;

import java.util.function.Consumer;

public class DoubleSetting extends Setting<Double> {

	@Getter
	private final double min;

	@Getter
	private final double max;

	@Getter
	private final double sliderMin;

	@Getter
	private final double sliderMax;

	@Getter
	private final boolean onSliderRelease;

	@Getter
	private final int decimalPlaces;

	@Getter
	private final boolean noSlider;

	private DoubleSetting(
			String id,
			String group,
			double defaultValue,
			Consumer<Double> onChanged,
			Consumer<Setting<Double>> onModuleActivated,
			IVisible visible,
			double min,
			double max,
			double sliderMin,
			double sliderMax,
			boolean onSliderRelease,
			int decimalPlaces,
			boolean noSlider
	) {
		super(id, group, defaultValue, onChanged, onModuleActivated, visible);

		this.min = min;
		this.max = max;
		this.sliderMin = sliderMin;
		this.sliderMax = sliderMax;
		this.decimalPlaces = decimalPlaces;
		this.onSliderRelease = onSliderRelease;
		this.noSlider = noSlider;
	}

	@Override
	protected Double parseImpl(String str) {
		try {
			return Double.parseDouble(str.trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	@Override
	protected boolean isValueValid(Double value) {
		return value >= min && value <= max;
	}

	@Override
	public JsonElement writeValue() {
		return new JsonPrimitive(get());
	}

	@Override
	public void readValue(JsonElement element) {
		set(element.getAsDouble());
	}

	public static Builder create(String id) {
		return new Builder(id);
	}

	public static class Builder extends SettingBuilder<Builder, Double, DoubleSetting> {

		private double min = Double.NEGATIVE_INFINITY;
		private double max = Double.POSITIVE_INFINITY;
		private double sliderMin = 0;
		private double sliderMax = 10;
		private boolean onSliderRelease = false;
		private int decimalPlaces = 3;
		private boolean noSlider = false;

		private Builder(String id) {
			super(id, 0D);
		}

		public Builder defaultValue(double defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		public Builder min(double min) {
			this.min = min;
			return this;
		}

		public Builder range(double min, double max) {
			this.min = Math.min(min, max);
			this.max = Math.max(min, max);
			return this;
		}

		public Builder sliderRange(double min, double max) {
			this.sliderMin = min;
			this.sliderMax = max;
			return this;
		}

		public Builder decimalPlaces(int decimalPlaces) {
			this.decimalPlaces = decimalPlaces;
			return this;
		}

		public Builder onSliderRelease() {
			this.onSliderRelease = true;
			return this;
		}

		public Builder noSlider() {
			this.noSlider = true;
			return this;
		}

		@Override
		public DoubleSetting done() {
			return new DoubleSetting(
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
					onSliderRelease,
					decimalPlaces,
					noSlider
			);
		}

	}

}
