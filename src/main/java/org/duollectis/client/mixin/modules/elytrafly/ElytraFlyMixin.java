package org.duollectis.client.mixin.modules.elytrafly;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.ElytraFly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает {@link LivingEntity#travel(Vec3d)} для подмены физики элитры.
 * <p>
 * В режиме {@link ElytraFly.FlightMode#CREATIVE} полностью отменяет стандартный
 * расчёт аэродинамики элитры: скорость уже выставлена модулем в {@code TickPreEvent},
 * поэтому здесь достаточно вызвать {@code move()} с текущей скоростью и заблокировать
 * дальнейшее выполнение {@code travel()}, чтобы ванильный код не перезаписал вектор.
 * <p>
 * В режиме {@link ElytraFly.FlightMode#VANILLA} добавляет импульс по вектору взгляда
 * при нажатии пробела — аналог фейерверка без расхода предметов.
 */
@Mixin(LivingEntity.class)
public class ElytraFlyMixin {

	@Inject(
		method = "travel",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$overrideElytraTravel(Vec3d movementInput, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;

		if (!(self instanceof ClientPlayerEntity player)) {
			return;
		}

		ElytraFly module = ModuleManager.getInstance().get(ElytraFly.class);

		if (module == null || !module.isActive()) {
			return;
		}

		if (!player.isGliding()) {
			return;
		}

		if (module.getMode() == ElytraFly.FlightMode.CREATIVE) {
			// Скорость уже выставлена в TickPreEvent — просто применяем движение
			// и блокируем ванильную аэродинамику элитры.
			player.move(MovementType.SELF, player.getVelocity());
			ci.cancel();
			return;
		}

		if (module.getMode() == ElytraFly.FlightMode.VANILLA) {
			applyVanillaBoost(player, module.getVanillaBoost());
		}
	}

	/**
	 * Добавляет импульс по вектору взгляда при нажатии пробела в режиме VANILLA.
	 * Не отменяет стандартную физику — лишь добавляет дополнительную скорость.
	 *
	 * @param player    клиентский игрок
	 * @param boostMult множитель силы буста из настроек модуля
	 */
	private void applyVanillaBoost(ClientPlayerEntity player, double boostMult) {
		if (!player.getAbilities().flying && player.input.playerInput.jump()) {
			float yawRad = player.getYaw() * (float) (Math.PI / 180.0);
			float pitchRad = player.getPitch() * (float) (Math.PI / 180.0);

			double lookX = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
			double lookY = -MathHelper.sin(pitchRad);
			double lookZ = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

			Vec3d boost = new Vec3d(lookX, lookY, lookZ).multiply(boostMult);
			player.addVelocity(boost.x, boost.y, boost.z);
		}
	}

}
