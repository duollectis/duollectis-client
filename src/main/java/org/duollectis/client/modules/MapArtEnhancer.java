package org.duollectis.client.modules;

import net.minecraft.client.texture.MapTextureManager;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.settings.IntSetting;

/**
 * Улучшает визуальное отображение мапартов на картах предметов.
 * Применяет LINEAR-фильтрацию GPU, размытие по Гауссу и постобработку пикселей
 * (яркость, контраст, насыщенность) прямо в NativeImage перед загрузкой текстуры на GPU.
 */
@ModuleInfo(
	value = "mapart-enhancer",
	category = Category.RENDER
)
public class MapArtEnhancer extends Module {

	public final Setting<Double> blur = settings.add(DoubleSetting
		.create("blur")
		.defaultValue(0.0)
		.range(0.0, 5.0)
		.sliderRange(0.0, 5.0)
		.decimalPlaces(1)
		.suffix("px")
		.done()
	);

	public final Setting<Integer> brightness = settings.add(IntSetting
		.create("brightness")
		.defaultValue(0)
		.range(-100, 100)
		.sliderRange(-100, 100)
		.suffix("%")
		.done()
	);

	public final Setting<Integer> contrast = settings.add(IntSetting
		.create("contrast")
		.defaultValue(0)
		.range(-100, 100)
		.sliderRange(-100, 100)
		.suffix("%")
		.done()
	);

	public final Setting<Integer> saturation = settings.add(IntSetting
		.create("saturation")
		.defaultValue(0)
		.range(-100, 100)
		.sliderRange(-100, 100)
		.suffix("%")
		.done()
	);

	@Override
	public void onDeactivate() {
		mc.getMapTextureManager().clear();
	}

}
