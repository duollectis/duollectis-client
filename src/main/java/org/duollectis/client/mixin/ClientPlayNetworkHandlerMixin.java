package org.duollectis.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import org.duollectis.client.utils.network.PacketUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для {@link ClientPlayNetworkHandler}: регистрирует ссылку на обработчик
 * в {@link org.duollectis.client.utils.network.PacketUtil} сразу после его создания,
 * чтобы утилита могла отправлять sequenced-пакеты.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

	@Inject(
		method = "<init>",
		at = @At("TAIL")
	)

	private void init(
			MinecraftClient client,
			ClientConnection clientConnection,
			ClientConnectionState clientConnectionState,
			CallbackInfo ci
	) {
		PacketUtil.setPlayNetworkHandler((ClientPlayNetworkHandler) (Object) this);
	}
}
