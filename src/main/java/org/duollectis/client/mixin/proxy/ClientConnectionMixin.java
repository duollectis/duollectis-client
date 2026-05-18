package org.duollectis.client.mixin.proxy;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.PacketSizeLogger;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

	@Inject(
			method = "addHandlers",
			at = @At("RETURN")
	)
	private static void onAddHandlers(
			ChannelPipeline pipeline,
			NetworkSide side,
			boolean local,
			PacketSizeLogger packetSizeLogger,
			CallbackInfo ci
	) {
		if (side != NetworkSide.CLIENTBOUND || local)
			return;

		ProxyManager pm = ProxyManager.getInstance();
		if (pm == null) return;

		Proxy proxy = pm.getActiveProxy();
		if (proxy == null) return;

		System.out.println("Add proxy: " + proxy.getAddress() + ":" + proxy.getPort());

		switch (proxy.getType()) {
			case SOCKS4 -> pipeline.addFirst(new Socks4ProxyHandler(
					new InetSocketAddress(proxy.getAddress(), proxy.getPort()),
					proxy.getUsername()
			));
			case SOCKS5 -> pipeline.addFirst(new Socks5ProxyHandler(
					new InetSocketAddress(proxy.getAddress(), proxy.getPort()),
					proxy.getUsername(),
					proxy.getPassword()
			));
		}
	}
}
