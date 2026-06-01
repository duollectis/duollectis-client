package org.duollectis.client.mixin.modules.zombiepickup;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.ZombieItemPickup;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(MobEntity.class)
public class ZombieEntityMixin {

	private static final Logger log = LoggerFactory.getLogger(ZombieEntityMixin.class);

	@Unique
	@Nullable
	private ItemStack stack;

	/**
	 * Принудительно разрешает подбор предметов всем зомби в сингл-плеере,
	 * когда модуль активен. Оригинальный шанс спавна (55%) заменяется на 100%.
	 */
	@Inject(
			method = "canPickUpLoot",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$forcePickupLoot(CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof ZombieEntity)) {
			return;
		}

		ZombieItemPickup module = ModuleManager.getInstance().get(ZombieItemPickup.class);

		if (module == null || !module.isActive()) {
			return;
		}

		cir.setReturnValue(true);
	}

	@Inject(
			method = "dropEquipment",
			at = @At("HEAD")
	)
	private void duollectis$setItem(
			ServerWorld world,
			DamageSource source,
			boolean causedByPlayer,
			CallbackInfo ci
	) {
		if ((Object) this instanceof ZombieEntity zombie) {
			updateStack(zombie, EquipmentSlot.MAINHAND);
			if (stack == null) {
				updateStack(zombie, EquipmentSlot.OFFHAND);
			}
		}
	}

	@Unique
	private void updateStack(ZombieEntity zombie, EquipmentSlot slot) {
		if (zombie.getEquipmentDropChances().dropsExactly(slot)) {
			ItemStack stack = zombie.getEquippedStack(slot);
			if (!stack.isEmpty()) {
				this.stack = stack;
			}
		}
	}

	/**
	 * После стандартного дропа снаряжения при смерти зомби проверяет условия дюпа:
	 * убийца — игрок, зомби имеет кастомное имя. При выполнении условий с шансом 23%
	 * дюпирует каждый предмет из слотов MAINHAND и OFFHAND, который зомби подобрал
	 * с земли (dropChance > 1.0, выставляется через equipLootStack → setDropGuaranteed).
	 */
	@Inject(
			method = "dropEquipment",
			at = @At("TAIL")
	)
	private void duollectis$dupePickedUpItems(
			ServerWorld world,
			DamageSource source,
			boolean causedByPlayer,
			CallbackInfo ci
	) {
		if (!((Object) this instanceof ZombieEntity zombie)) {
			return;
		}

		if (stack == null) {
			return;
		}

		ZombieItemPickup module = ModuleManager.getInstance().get(ZombieItemPickup.class);

		if (module == null || !module.isActive()) {
			return;
		}

		if (!causedByPlayer) {
			return;
		}

		if (!zombie.hasCustomName()) {
			return;
		}

		if (!(source.getAttacker() instanceof PlayerEntity)) {
			return;
		}

		double value = new Random().nextDouble(0, 1);

		if (value <= module.getChance()) {
			ItemEntity duped = new ItemEntity(
					world,
					zombie.getX(),
					zombie.getY(),
					zombie.getZ(),
					stack.copy()
			);

			world.spawnEntity(duped);
		}
	}

}
