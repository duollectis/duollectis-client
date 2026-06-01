package org.duollectis.client.mixin.modules.noworldsave;

import net.minecraft.server.world.ServerChunkManager;
import org.duollectis.client.modules.NoWorldSave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkManager.class)
public class ServerChunkManagerSaveMixin {

	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
	private void duollectis$blockChunkManagerSave(boolean flush, CallbackInfo ci) {
		if (NoWorldSave.isBlockingSave()) {
			ci.cancel();
		}
	}

}
