package org.duollectis.client.mixin.modules.freecam;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает {@link Camera#update} в TAIL и подменяет позицию/ориентацию камеры
 * на значения из {@link FreeCam}, когда модуль активен.
 * <p>
 * Позиция интерполируется между {@code prevCameraPos} и {@code cameraPos}
 * через {@code tickProgress}, что устраняет рывки при движении камеры.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

	@Inject(
		method = "update",
		at = @At("TAIL")
	)
	private void duollectis$applyFreeCam(
		World area,
		Entity focusedEntity,
		boolean thirdPerson,
		boolean inverseView,
		float tickProgress,
		CallbackInfo ci
	) {
		FreeCam module = ModuleManager.getInstance().get(FreeCam.class);

		if (module == null || !module.isActive()) {
			return;
		}

		Camera camera = (Camera) (Object) this;
		Vec3d prev = module.getPrevCameraPos();
		Vec3d curr = module.getCameraPos();

		Vec3d interpolated = new Vec3d(
			MathHelper.lerp(tickProgress, prev.x, curr.x),
			MathHelper.lerp(tickProgress, prev.y, curr.y),
			MathHelper.lerp(tickProgress, prev.z, curr.z)
		);

		camera.setPos(interpolated);
		camera.setRotation(module.getCameraYaw(), module.getCameraPitch());
	}

}
