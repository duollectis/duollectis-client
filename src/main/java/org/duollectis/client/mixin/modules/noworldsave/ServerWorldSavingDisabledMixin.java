package org.duollectis.client.mixin.modules.noworldsave;

import net.minecraft.server.world.ServerWorld;
import org.duollectis.client.modules.NoWorldSave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldSavingDisabledMixin {

	/**
	 * Блокирует сохранение чанков через флаг isSavingDisabled только во время
	 * активной игровой сессии. Во время shutdown (isRunning() == false) сервер
	 * должен свободно выгружать чанки, иначе цикл shouldDelayShutdown() зависнет.
	 */
	@Inject(
			method = "isSavingDisabled",
			at = @At("HEAD"),
			cancellable = true
	)
	private void duollectis$forceSavingDisabled(CallbackInfoReturnable<Boolean> cir) {
		ServerWorld world = (ServerWorld) (Object) this;

		if (NoWorldSave.isBlockingSave() && world.getServer().isRunning()) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}

}
