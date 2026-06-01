package org.duollectis.client.mixin.modules.freecam;

import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Перехватывает вызов {@link ClientPlayerEntity#changeLookDirection} внутри
 * {@link Mouse#updateMouse}, когда активен {@link FreeCam}.
 * <p>
 * Вместо поворота тела игрока дельты мыши передаются в {@code FreeCam#updateRotation},
 * что вращает только свободную камеру. Игрок остаётся смотреть в исходном направлении.
 */
@Mixin(Mouse.class)
public class FreeCamMouseMixin {

	@Redirect(
		method = "updateMouse",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
		)
	)
	private void duollectis$redirectLookDirection(
		ClientPlayerEntity player,
		double deltaYaw,
		double deltaPitch
	) {
		FreeCam module = ModuleManager.getInstance().get(FreeCam.class);

		if (module == null || !module.isActive()) {
			player.changeLookDirection(deltaYaw, deltaPitch);
			return;
		}

		module.updateRotation((float) deltaPitch, (float) deltaYaw);
	}

}
