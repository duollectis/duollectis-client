package org.duollectis.client.modules;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.PacketReceiveEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;

@ModuleInfo(
		value = "velocity",
		category = Category.MOVEMENT
)
public class VelocityModule extends Module {

	@Subscribe
	private void onPacketReceive(PacketReceiveEvent event) {
		if (mc.player == null) {
			return;
		}

		if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocityPacket
				&& velocityPacket.getEntityId() == mc.player.getId()
		) {
			event.cancel();
		}
	}

}
