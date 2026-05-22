package org.duollectis.client.mixin.events.render;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.RenderEvent;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.duollectis.client.utils.MC.mc;

/**
 * Mixin для {@link GameRenderer}: публикует {@link RenderEvent} непосредственно перед
 * финальным сбросом entity-буферов. В этот момент projection matrix ещё в world-режиме,
 * поэтому вершины, добавленные в {@code getEntityVertexConsumers()}, корректно
 * проецируются в мировое пространство.
 * Шейдер lines получает view/projection через RenderSystem — передаём identity-матрицу,
 * координаты вершин уже смещены на -cameraPos.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(
			method = "renderWorld",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V",
					ordinal = 0
			)
	)
	private void duollectis$onBeforeEntityDraw(RenderTickCounter renderTickCounter, CallbackInfo ci) {
		VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
		EventBus.getInstance().post(new RenderEvent(new MatrixStack(), (GameRenderer) (Object) this, immediate, new Matrix4f()));
	}

}
