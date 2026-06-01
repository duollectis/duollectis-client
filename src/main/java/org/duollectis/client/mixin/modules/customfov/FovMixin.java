package org.duollectis.client.mixin.modules.customfov;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.CustomFov;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Перехватывает приватный метод {@code getFov} в {@link GameRenderer} и подменяет
 * возвращаемое значение на плавно интерполированный FOV из {@link CustomFov}, когда модуль активен.
 */
@Mixin(GameRenderer.class)
public class FovMixin {

	@Inject(
		method = "getFov",
		at = @At("RETURN"),
		cancellable = true
	)
	private void duollectis$applyCustomFov(
		Camera camera,
		float tickProgress,
		boolean changingFov,
		CallbackInfoReturnable<Float> cir
	) {
		CustomFov module = ModuleManager.getInstance().get(CustomFov.class);

		if (module == null || !module.isActive()) {
			return;
		}

		cir.setReturnValue(module.getInterpolatedFov(tickProgress));
	}

}
