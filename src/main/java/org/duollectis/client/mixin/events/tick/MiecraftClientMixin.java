package org.duollectis.client.mixin.events.tick;

import net.minecraft.client.MinecraftClient;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.TickPostEvent;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для {@link net.minecraft.client.MinecraftClient}: публикует
 * {@link TickPreEvent} в начале и {@link TickPostEvent} в конце каждого игрового тика.
 */
@Mixin(MinecraftClient.class)
public class MiecraftClientMixin {

	@Inject(
			method = "tick",
			at = @At("HEAD")
	)
	private void duollectis$tickPre(CallbackInfo ci) {
		EventBus.getInstance().post(TickPreEvent.INSTANCE);
	}

	@Inject(
			method = "tick",
			at = @At("TAIL")
	)
	private void duollectis$tickPost(CallbackInfo ci) {
		EventBus.getInstance().post(TickPostEvent.INSTANCE);
	}

}
