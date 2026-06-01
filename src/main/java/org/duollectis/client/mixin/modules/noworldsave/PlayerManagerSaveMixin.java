package org.duollectis.client.mixin.modules.noworldsave;

import net.minecraft.server.PlayerManager;
import org.duollectis.client.modules.NoWorldSave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerSaveMixin {

	@Inject(
			method = "saveAllPlayerData",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$blockPlayerSave(CallbackInfo ci) {
		if (NoWorldSave.isBlockingSave()) {
			ci.cancel();
		}
	}

}
