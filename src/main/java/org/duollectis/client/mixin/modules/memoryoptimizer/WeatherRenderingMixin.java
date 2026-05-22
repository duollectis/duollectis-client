package org.duollectis.client.mixin.modules.memoryoptimizer;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MemoryOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Отключает построение геометрии осадков и спавн звуковых/визуальных частиц дождя.
 * Управляется настройками {@code no-weather} и {@code no-weather-particles}
 * модуля {@link MemoryOptimizer}.
 */
@Mixin(WeatherRendering.class)
public abstract class WeatherRenderingMixin {

	@Inject(
		method = "buildPrecipitationPieces",
		at = @At("HEAD"),
		cancellable = true
	)
	private void skipPrecipitationBuild(
		World world,
		int ticks,
		float tickProgress,
		Vec3d cameraPos,
		WeatherRenderState state,
		CallbackInfo ci
	) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getNoWeather().get()) {
			return;
		}

		ci.cancel();
	}

	@Inject(
		method = "addParticlesAndSound",
		at = @At("HEAD"),
		cancellable = true
	)
	private void skipWeatherParticles(
		ClientWorld world,
		Camera camera,
		int ticks,
		ParticlesMode particlesMode,
		int weatherRadius,
		CallbackInfo ci
	) {
		MemoryOptimizer module = ModuleManager.getInstance().get(MemoryOptimizer.class);

		if (module == null || !module.isActive() || !module.getNoWeatherParticles().get()) {
			return;
		}

		ci.cancel();
	}

}
