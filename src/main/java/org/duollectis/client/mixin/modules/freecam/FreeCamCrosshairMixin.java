package org.duollectis.client.mixin.modules.freecam;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.duollectis.client.utils.MC.mc;

/**
 * Перенаправляет raycast прицела с позиции игрока на позицию свободной камеры FreeCam.
 * Позволяет ставить и ломать блоки от лица камеры, а не от лица персонажа.
 */
@Mixin(GameRenderer.class)
public class FreeCamCrosshairMixin {

	@Inject(method = "updateCrosshairTarget", at = @At("HEAD"), cancellable = true)
	private void duollectis$overrideCrosshairTarget(float tickProgress, CallbackInfo ci) {
		FreeCam module = ModuleManager.getInstance().get(FreeCam.class);

		if (module == null || !module.isActive()) {
			return;
		}

		ClientPlayerEntity player = mc.player;

		if (player == null || mc.world == null) {
			return;
		}

		mc.crosshairTarget = computeFreeCamHitResult(player, module);
		mc.targetedEntity = mc.crosshairTarget instanceof EntityHitResult entityHit
			? entityHit.getEntity()
			: null;

		ci.cancel();
	}

	/**
	 * Выполняет raycast от позиции свободной камеры FreeCam с учётом дальности взаимодействия игрока.
	 * Проверяет как блоки, так и сущности — аналогично ванильному {@code raycastWithEntityDetection}.
	 *
	 * @param player игрок, у которого берётся дальность взаимодействия
	 * @param module активный модуль FreeCam с позицией и направлением камеры
	 * @return результат попадания от позиции камеры FreeCam
	 */
	private static HitResult computeFreeCamHitResult(ClientPlayerEntity player, FreeCam module) {
		double blockRange = player.getBlockInteractionRange();
		double entityRange = player.getEntityInteractionRange();
		double maxRange = Math.max(blockRange, entityRange);

		Vec3d cameraPos = module.getCameraPos();
		Vec3d lookVec = module.getCameraLookVec();
		Vec3d endPos = cameraPos.add(lookVec.multiply(maxRange));

		ClientWorld world = mc.world;
		BlockHitResult blockHit = world.raycast(new RaycastContext(
			cameraPos,
			endPos,
			RaycastContext.ShapeType.OUTLINE,
			RaycastContext.FluidHandling.NONE,
			player
		));

		double blockDistSq = blockHit.getPos().squaredDistanceTo(cameraPos);
		double effectiveRange = blockHit.getType() != HitResult.Type.MISS
			? Math.sqrt(blockDistSq)
			: maxRange;

		Vec3d entityEndPos = cameraPos.add(lookVec.multiply(effectiveRange));
		Box sweepBox = player.getBoundingBox()
			.stretch(lookVec.multiply(effectiveRange))
			.expand(1.0, 1.0, 1.0);

		EntityHitResult entityHit = ProjectileUtil.raycast(
			player,
			cameraPos,
			entityEndPos,
			sweepBox,
			e -> !e.isSpectator() && e.canHit(),
			effectiveRange * effectiveRange
		);

		if (entityHit != null && entityHit.getPos().squaredDistanceTo(cameraPos) < blockDistSq) {
			return clampToRange(entityHit, cameraPos, entityRange);
		}

		return clampToRange(blockHit, cameraPos, blockRange);
	}

	private static HitResult clampToRange(HitResult hit, Vec3d origin, double range) {
		if (hit.getPos().isInRange(origin, range)) {
			return hit;
		}

		Vec3d hitPos = hit.getPos();
		Vec3d delta = hitPos.subtract(origin);
		Direction direction = Direction.getFacing(delta.x, delta.y, delta.z);

		return BlockHitResult.createMissed(hitPos, direction, BlockPos.ofFloored(hitPos));
	}

}
