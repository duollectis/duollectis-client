package org.duollectis.client.mixin.modules.noworldsave;

import net.minecraft.server.world.ServerEntityManager;
import org.duollectis.client.modules.NoWorldSave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntityManager.class)
public class ServerEntityManagerSaveMixin {

	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
	private void duollectis$blockEntitySave(CallbackInfo ci) {
		if (NoWorldSave.isBlockingSave()) {
			ci.cancel();
		}
	}

	@Inject(method = "flush", at = @At("HEAD"), cancellable = true)
	private void duollectis$blockEntityFlush(CallbackInfo ci) {
		if (NoWorldSave.isBlockingSave()) {
			ci.cancel();
		}
	}

}
