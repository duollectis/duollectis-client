package org.duollectis.client.mixin.modules.noworldsave;

import net.minecraft.server.MinecraftServer;
import org.duollectis.client.modules.NoWorldSave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerSaveMixin {

	@Inject(method = "save(ZZZ)Z", at = @At("HEAD"), cancellable = true)
	private void duollectis$blockSave(
			boolean suppressLogs,
			boolean flush,
			boolean force,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (NoWorldSave.isBlockingSave()) {
			cir.setReturnValue(false);
			cir.cancel();
		}
	}

	@Inject(method = "saveAll(ZZZ)Z", at = @At("HEAD"), cancellable = true)
	private void duollectis$blockSaveAll(
			boolean suppressLogs,
			boolean flush,
			boolean force,
			CallbackInfoReturnable<Boolean> cir
	) {
		if (NoWorldSave.isBlockingSave()) {
			cir.setReturnValue(false);
			cir.cancel();
		}
	}

}
