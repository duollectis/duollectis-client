package org.duollectis.client.mixin.modules.mapart;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.mapartbuilder.MapArtBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает {@code travel()} у {@link LivingEntity} пока активен {@link MapArtBuilder}.
 * <p>
 * Когда игрок летит на элитрах, полностью блокирует ванильную аэродинамику и применяет
 * желаемую velocity из {@link MapArtBuilder#getDesiredVelocity()} напрямую через
 * {@code move(SELF, velocity)}, после чего обнуляет velocity игрока.
 * Это гарантирует что ни гравитация, ни аэродинамика элитр не влияют на движение.
 */
@Mixin(LivingEntity.class)
public class ClientPlayerEntityMixin {

	@Inject(
		method = "travel",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$overrideElytraFlight(Vec3d movementInput, CallbackInfo ci) {
		if (!((Object) this instanceof ClientPlayerEntity player)) {
			return;
		}

		if (!player.isGliding()) {
			return;
		}

		var module = ModuleManager.getInstance().get(MapArtBuilder.class);

		if (module == null || !module.isActive()) {
			return;
		}

		player.setVelocity(module.getVelocity());
		player.move(MovementType.SELF, module.getVelocity());
		player.setVelocity(Vec3d.ZERO);

		ci.cancel();
	}

}
