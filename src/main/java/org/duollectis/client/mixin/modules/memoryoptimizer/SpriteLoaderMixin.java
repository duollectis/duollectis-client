package org.duollectis.client.mixin.modules.memoryoptimizer;

import net.minecraft.client.texture.SpriteLoader;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MemoryOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Принудительно обнуляет уровень mipmap при сшивке спрайт-атласа.
 * Каждый уровень mipmap хранит уменьшенную копию всего атласа в памяти:
 * уровень 1 = 1/4 атласа, уровень 2 = 1/16 и т.д. — суммарно ~33% от размера оригинала.
 * При mipLevel=0 все эти копии не создаются — экономия ~100-200 МБ heap.
 * Требует перезагрузки ресурсов (F3+T) для применения.
 */
@Mixin(SpriteLoader.class)
public abstract class SpriteLoaderMixin {

	@ModifyVariable(
		method = "stitch(Ljava/util/List;ILjava/util/concurrent/Executor;)Lnet/minecraft/client/texture/SpriteLoader$StitchResult;",
		at = @At("HEAD"),
		argsOnly = true,
		index = 2
	)
	private int forceMipLevelZero(int mipLevel) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getNoMipmap().get()) {
			return mipLevel;
		}

		return 0;
	}

}
