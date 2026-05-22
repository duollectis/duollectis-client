package org.duollectis.client.api.event.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.duollectis.client.api.event.Event;
import org.joml.Matrix4f;

/** Событие рендера мира, публикуемое перед финальным сбросом entity-буферов. */
@Getter
@RequiredArgsConstructor
public final class RenderEvent implements Event {

	private final MatrixStack matrices;
	private final GameRenderer gameRenderer;
	private final VertexConsumerProvider consumers;
	/**
	 * View-матрица мира (rotation камеры). Необходима для корректного
	 * позиционирования примитивов в мировом пространстве через MatrixStack.
	 */
	private final Matrix4f modelView;

}
