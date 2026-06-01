package org.duollectis.client.mixin.modules.noworldsave;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import org.duollectis.client.modules.NoWorldSave;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldSaveMixin {

	@Inject(
			method = "save(Lnet/minecraft/util/ProgressListener;ZZ)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$blockChunkSave(
			@Nullable ProgressListener progressListener,
			boolean flush,
			boolean savingDisabled,
			CallbackInfo ci
	) {
		if (NoWorldSave.isBlockingSave()) {
			ci.cancel();
		}
	}

}
