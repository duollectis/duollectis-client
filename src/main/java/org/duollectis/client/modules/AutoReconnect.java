package org.duollectis.client.modules;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import lombok.Getter;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.GameJoinedEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.DoubleSetting;

@ModuleInfo(
		value = "auto-reconnect",
		category = Category.MISC
)
/**
 * Модуль автоматического переподключения к серверу после разрыва соединения.
 * Запоминает последний подключённый сервер через {@link GameJoinedEvent}
 * и используется {@code DisconnectedScreenMixin} для обратного отсчёта и переподключения.
 */
public class AutoReconnect extends Module {

	@Getter
	private final Setting<Double> time = settings.add(DoubleSetting
			.create("delay")
			.defaultValue(3.5)
			.min(0)
			.decimalPlaces(1)
			.done());

	@Getter
	private final Setting<Boolean> button = settings.add(BoolSetting
			.create("hide-buttons")
			.defaultValue(false)
			.done());

	@Getter
	private Pair<ServerAddress, ServerInfo> lastServerConnection;

	@Subscribe
	private void onGameJoined(GameJoinedEvent event) {
		lastServerConnection = new ObjectObjectImmutablePair<>(event.getAddress(), event.getInfo());
	}

}
