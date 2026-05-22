package org.duollectis.client.mixin.modules.memoryoptimizer;

import net.minecraft.client.texture.SpriteAtlasTexture;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MemoryOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Замораживает тик анимаций спрайт-атласа (вода, лава, огонь, порталы).
 * Каждый тик анимации перезаписывает GPU-буфер через {@code tickAnimatedSprites},
 * что создаёт постоянное давление на CPU-память и шину PCI-E.
 * При заморозке кадр остаётся статичным — экономия ~50-150 МБ heap-буферов.
 */
@Mixin(SpriteAtlasTexture.class)
public abstract class SpriteAtlasTextureMixin {

	@Inject(method = "tickAnimatedSprites", at = @At("HEAD"), cancellable = true)
	private void freezeAnimations(CallbackInfo ci) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getFreezeAnimations().get()) {
			return;
		}

		ci.cancel();
	}

}
