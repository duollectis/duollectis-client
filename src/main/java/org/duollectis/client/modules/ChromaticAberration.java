package org.duollectis.client.modules;

import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.utils.render.ChromaticAberrationRenderer;

@ModuleInfo(
	value = "chromatic-aberration",
	category = Category.RENDER
)
public class ChromaticAberration extends Module {

	private final DoubleSetting strength = settings.add(
		DoubleSetting.create("strength")
			.defaultValue(2.0)
			.range(0.1, 10.0)
			.sliderRange(0.1, 5.0)
			.decimalPlaces(1)
			.done()
	);

	private final ChromaticAberrationRenderer renderer = new ChromaticAberrationRenderer();

	@Subscribe
	private void onRender(RenderEvent event) {
		renderer.apply(strength.get().floatValue());
	}

	@Override
	public void onDeactivate() {
		renderer.close();
	}

}
