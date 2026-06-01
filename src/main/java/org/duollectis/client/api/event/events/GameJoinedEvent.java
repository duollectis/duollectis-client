package org.duollectis.client.api.event.events;

import lombok.Getter;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.duollectis.client.api.event.Event;

/** Событие, публикуемое в момент начала подключения к серверу (до завершения рукопожатия). */
@Getter
public final class GameJoinedEvent implements Event {

	public static final GameJoinedEvent INSTANCE = new GameJoinedEvent();

	private ServerAddress address;
	private ServerInfo info;

	private GameJoinedEvent() {}

	public void set(ServerAddress address, ServerInfo info) {
		this.address = address;
		this.info = info;
	}

}
