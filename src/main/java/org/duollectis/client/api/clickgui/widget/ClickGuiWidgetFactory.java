package org.duollectis.client.api.clickgui.widget;

import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.ColorSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.settings.EnumSetting;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.settings.ItemListSetting;

/**
 * Фабрика виджетов настроек ClickGui.
 * Создаёт подходящий виджет по типу настройки.
 */
public final class ClickGuiWidgetFactory {

	private ClickGuiWidgetFactory() {}

	/**
	 * Создаёт виджет для переданной настройки.
	 * Порядок проверки: Bool → Slider (Int/Double с isSlider) → Enum → Color → ItemList → fallback.
	 */
	public static ClickGuiSettingWidget create(Setting<?> setting) {
		if (setting instanceof BoolSetting boolSetting) {
			return new BoolWidget(boolSetting);
		}

		if (setting instanceof IntSetting intSetting && intSetting.isSlider()) {
			return new SliderWidget(intSetting);
		}

		if (setting instanceof DoubleSetting doubleSetting && doubleSetting.isSlider()) {
			return new SliderWidget(doubleSetting);
		}

		if (setting instanceof EnumSetting<?> enumSetting) {
			return new EnumWidget(enumSetting);
		}

		if (setting instanceof ColorSetting colorSetting) {
			return new ColorWidget(colorSetting);
		}

		if (setting instanceof ItemListSetting itemListSetting) {
			return new ItemListWidget(itemListSetting);
		}

		return new FallbackWidget(setting);
	}
}
