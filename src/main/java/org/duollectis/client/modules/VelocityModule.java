package org.duollectis.client.modules;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.duollectis.client.api.event.Cancellable;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;

@ModuleInfo(
		name = "velocity",
		description = "Cancel knockbacks packets for client player."
)
public class VelocityModule extends Module {

	@Override
	public void onPacketReceive(Packet<?> _packet, Cancellable cancellable) {
		if (mc.player == null) {
			return;
		}

		if (_packet instanceof EntityVelocityUpdateS2CPacket packet) {

			if (packet.getEntityId() == mc.player.getId()) {
				cancellable.cancel();
			}
		}
	}
}
