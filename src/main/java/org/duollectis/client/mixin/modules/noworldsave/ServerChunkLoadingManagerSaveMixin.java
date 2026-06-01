package org.duollectis.client.mixin.modules.noworldsave;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.duollectis.client.modules.NoWorldSave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkLoadingManager.class)
public class ServerChunkLoadingManagerSaveMixin {

	@Inject(method = "save(Z)V", at = @At("HEAD"), cancellable = true)
	private void duollectis$blockFlushSave(boolean flush, CallbackInfo ci) {
		if (NoWorldSave.isBlockingSave()) {
			ci.cancel();
		}
	}

	/**
	 * Перехватывает приватный save(Chunk), вызываемый из tryUnloadChunk() при выгрузке чанков.
	 * Без этого перехвата чанки сохраняются даже при заблокированном сохранении.
	 */
	@Inject(
			method = "save(Lnet/minecraft/world/chunk/Chunk;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$blockChunkSave(Chunk chunk, CallbackInfoReturnable<Boolean> cir) {
		if (NoWorldSave.isBlockingSave()) {
			cir.setReturnValue(false);
			cir.cancel();
		}
	}

}
