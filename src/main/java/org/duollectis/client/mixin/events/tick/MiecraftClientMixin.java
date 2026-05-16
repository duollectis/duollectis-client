package org.duollectis.client.mixin.events.tick;

import net.minecraft.client.MinecraftClient;
import org.duollectis.client.DuollectisMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MiecraftClientMixin {

	@Inject(
			method = "tick",
			at = @At("HEAD")
	)
	private void duollectis$tickPre(CallbackInfo ci) {
		DuollectisMod.getEventManager().onTickPre();
	}

	@Inject(
			method = "tick",
			at = @At("TAIL")
	)
	private void duollectis$tickPost(CallbackInfo ci) {
		DuollectisMod.getEventManager().onTickPost();
	}
}
