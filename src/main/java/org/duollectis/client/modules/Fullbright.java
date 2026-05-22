package org.duollectis.client.modules;

import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.DoubleSetting;

@ModuleInfo(
		value = "fullbright",
		category = Category.RENDER
)
public class Fullbright extends Module {

	private final Setting<Double> value = settings.add(DoubleSetting
			.create("value")
			.min(0)
			.sliderRange(0, 10)
			.done());

	public float getValue() {
		return value.get().floatValue();
	}
}
