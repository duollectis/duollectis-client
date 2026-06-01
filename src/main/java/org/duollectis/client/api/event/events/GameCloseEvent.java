package org.duollectis.client.api.event.events;

import org.duollectis.client.api.event.Event;

public class GameCloseEvent implements Event {

	public static final GameCloseEvent INSTANCE = new GameCloseEvent();

	private GameCloseEvent() {}
}
