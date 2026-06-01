package org.duollectis.client.mixin.events.close;

import net.minecraft.client.MinecraftClient;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.GameCloseEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(
		method = "close",
		at = @At("HEAD")
	)
	private void duollectis$saveOnClose(CallbackInfo ci) {
		EventBus.getInstance().post(GameCloseEvent.INSTANCE);
	}
}
