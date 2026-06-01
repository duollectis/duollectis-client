package org.duollectis.client.mixin.modules.noviewbob;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.NoViewBob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Полностью замораживает камеру при активном {@link NoViewBob}:
 * <ul>
 *   <li>Отменяет покачивание при ходьбе ({@code bobView}).</li>
 *   <li>Отменяет наклон при уроне и смерти ({@code tiltViewWhenHurt}).</li>
 *   <li>Обнуляет эффект тошноты/зелья ({@code renderWorld} — nausea-блок).</li>
 * </ul>
 */
@Mixin(GameRenderer.class)
public class ViewBobMixin {

	@Inject(
		method = "bobView",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$cancelViewBob(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
		if (isActive()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "tiltViewWhenHurt",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$cancelTiltWhenHurt(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
		if (isActive()) {
			ci.cancel();
		}
	}

	/**
	 * Обнуляет итоговый коэффициент тошноты {@code k} в {@code renderWorld},
	 * чтобы nausea-вращение матрицы проекции не применялось.
	 * Переменная {@code k = Math.max(i, j) * (h * h)} — третья float-переменная в методе.
	 */
	@ModifyVariable(
		method = "renderWorld",
		at = @At(
			value = "STORE"
		),
		ordinal = 4
	)
	private float duollectis$cancelNausea(float nauseaFactor) {
		return isActive() ? 0.0f : nauseaFactor;
	}

	private static boolean isActive() {
		NoViewBob module = ModuleManager.getInstance().get(NoViewBob.class);
		return module != null && module.isActive();
	}

}
