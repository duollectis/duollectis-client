package org.duollectis.client.modules;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.settings.DoubleSetting;

@ModuleInfo(
	value = "custom-fov",
	category = Category.RENDER
)
public class CustomFov extends Module {

	private static final float INTERPOLATION_SPEED = 0.15f;

	@Getter
	private float currentFov;

	private final DoubleSetting fov = settings.add(
		DoubleSetting.create("fov")
			.defaultValue(90)
			.range(1, 179)
			.sliderRange(30, 150)
			.decimalPlaces(0)
			.suffix("°")
			.done()
	);

	@Override
	public void onActivate() {
		currentFov = fov.get().floatValue();
	}

	/**
	 * Плавно интерполирует текущий FOV к целевому значению.
	 * Вызывается из {@code FovMixin} каждый кадр через перехват {@code GameRenderer#getFov}.
	 */
	public float getInterpolatedFov(float tickProgress) {
		float target = fov.get().floatValue();
		currentFov = MathHelper.lerp(INTERPOLATION_SPEED, currentFov, target);
		return currentFov;
	}

}
