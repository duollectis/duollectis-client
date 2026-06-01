package org.duollectis.client.modules;

import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.utils.render.BloomRenderer;

@ModuleInfo(
	value = "bloom",
	category = Category.RENDER
)
public class Bloom extends Module {

	private final DoubleSetting threshold = settings.add(
		DoubleSetting.create("threshold")
			.defaultValue(0.6)
			.range(0.0, 1.0)
			.sliderRange(0.0, 1.0)
			.decimalPlaces(2)
			.done()
	);

	private final DoubleSetting intensity = settings.add(
		DoubleSetting.create("intensity")
			.defaultValue(1.5)
			.range(0.1, 10.0)
			.sliderRange(0.1, 5.0)
			.decimalPlaces(2)
			.done()
	);

	private final DoubleSetting radius = settings.add(
		DoubleSetting.create("radius")
			.defaultValue(4.0)
			.range(1.0, 20.0)
			.sliderRange(1.0, 12.0)
			.decimalPlaces(1)
			.done()
	);

	private final BloomRenderer renderer = new BloomRenderer();

	@Subscribe
	private void onRender(RenderEvent event) {
		renderer.apply(
			threshold.get().floatValue(),
			intensity.get().floatValue(),
			radius.get().floatValue()
		);
	}

	@Override
	public void onDeactivate() {
		renderer.close();
	}

}
