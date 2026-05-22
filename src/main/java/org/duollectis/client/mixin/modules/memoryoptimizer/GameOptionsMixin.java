package org.duollectis.client.mixin.modules.memoryoptimizer;

import net.minecraft.client.option.GameOptions;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MemoryOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Принудительно ограничивает дистанцию рендера чанков.
 * Каждый дополнительный чанк рендера добавляет ~1-2 МБ GPU-буферов и CPU-данных секций.
 * При дистанции 2 вместо стандартных 12 экономия составляет ~200-400 МБ.
 * Перехватывается именно {@code getClampedViewDistance}, чтобы не трогать
 * сохранённое значение настройки — при отключении модуля дистанция восстановится.
 */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

	@Inject(method = "getClampedViewDistance", at = @At("RETURN"), cancellable = true)
	private void clampRenderDistance(CallbackInfoReturnable<Integer> cir) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getLimitRenderDistance().get()) {
			return;
		}

		int current = cir.getReturnValue();
		int limit = module.getMaxRenderDistance().get();

		if (current > limit) {
			cir.setReturnValue(limit);
		}
	}

}
