package org.duollectis.client.mixin.modules.memoryoptimizer;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MemoryOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ограничивает количество одновременно воспроизводимых звуков.
 * Каждый активный OpenAL-источник удерживает декодированный PCM-буфер в heap-памяти.
 * При превышении лимита новый звук отбрасывается, если его категория не является музыкой
 * (музыку всегда пропускаем, чтобы не ломать атмосферу).
 */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

	@Shadow
	private com.google.common.collect.Multimap<SoundCategory, SoundInstance> sounds;

	@Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
	private void limitActiveSounds(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getLimitSounds().get()) {
			return;
		}

		if (sound.getCategory() == SoundCategory.MUSIC) {
			return;
		}

		int activeCount = sounds.size();

		if (activeCount >= module.getMaxSounds().get()) {
			cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
		}
	}

}
