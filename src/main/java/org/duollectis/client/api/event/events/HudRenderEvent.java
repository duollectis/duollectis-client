package org.duollectis.client.api.event.events;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.duollectis.client.api.event.Event;

/** Событие рендера HUD, публикуемое в конце {@code InGameHud.render}. */
@Getter
public final class HudRenderEvent implements Event {

	public static final HudRenderEvent INSTANCE = new HudRenderEvent();

	private DrawContext context;
	private RenderTickCounter tickCounter;

	private HudRenderEvent() {}

	public void set(DrawContext context, RenderTickCounter tickCounter) {
		this.context = context;
		this.tickCounter = tickCounter;
	}

}
