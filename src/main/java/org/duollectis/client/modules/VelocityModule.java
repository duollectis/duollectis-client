package org.duollectis.client.modules;

import static org.duollectis.client.utils.MC.mc;

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
/**
 * Модуль отмены входящего кнокбека (velocity). Перехватывает пакет
 * {@link net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket}
 * и отменяет его, если он адресован локальному игроку.
 */
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
