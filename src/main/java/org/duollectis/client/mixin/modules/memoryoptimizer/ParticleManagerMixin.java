package org.duollectis.client.mixin.modules.memoryoptimizer;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleRenderer;
import net.minecraft.client.particle.ParticleTextureSheet;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MemoryOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Ограничивает суммарное количество активных частиц в мире.
 * Новые частицы отбрасываются, если пул уже заполнен до предела,
 * заданного настройкой {@code max-particles} модуля {@link MemoryOptimizer}.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

	@Shadow
	private Map<ParticleTextureSheet, ParticleRenderer<?>> particles;

	@Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
	private void limitParticlePool(Particle particle, CallbackInfo ci) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getLimitParticles().get()) {
			return;
		}

		int total = particles.values().stream()
			.mapToInt(ParticleRenderer::size)
			.sum();

		if (total >= module.getMaxParticles().get()) {
			ci.cancel();
		}
	}
}
