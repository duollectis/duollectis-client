package org.duollectis.client.modules;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.duollectis.client.DuollectisMod;
import org.duollectis.client.api.event.EventHandler;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.settings.Setting;
import org.duollectis.client.settings.SettingGroup;

@ModuleInfo(
		name = "auto-reconnect",
		description = "Automatically reconnects when disconnected from a server."
)
public class AutoReconnect extends Module {

	private final SettingGroup sgGeneral = settings.getDefaultGroup();

	public final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
			.name("delay")
			.description("The amount of seconds to wait before reconnecting to the server.")
			.defaultValue(3.5)
			.min(0)
			.decimalPlaces(1)
			.build()
	);

	public final Setting<Boolean> button = sgGeneral.add(new BoolSetting.Builder()
			.name("hide-buttons")
			.description("Will hide the buttons related to Auto Reconnect.")
			.defaultValue(false)
			.build()
	);

	public Pair<ServerAddress, ServerInfo> lastServerConnection;

	public AutoReconnect() {

		DuollectisMod.getEventManager().add(new EventHandler() {

			@Override
			public void onGameJoined(ServerAddress address, ServerInfo info) {
				lastServerConnection = new ObjectObjectImmutablePair<>(address, info);
			}
		});

	}
}
