package org.duollectis.client.api.event.events;

import lombok.Getter;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.duollectis.client.api.event.Event;

/** Событие рендера мира, публикуемое перед финальным сбросом entity-буферов. */
@Getter
public final class RenderEvent implements Event {

	public static final RenderEvent INSTANCE = new RenderEvent();

	private MatrixStack matrices;
	private GameRenderer gameRenderer;
	private VertexConsumerProvider consumers;

	private RenderEvent() {}

	public void set(MatrixStack matrices, GameRenderer gameRenderer, VertexConsumerProvider consumers) {
		this.matrices = matrices;
		this.gameRenderer = gameRenderer;
		this.consumers = consumers;
	}

}
