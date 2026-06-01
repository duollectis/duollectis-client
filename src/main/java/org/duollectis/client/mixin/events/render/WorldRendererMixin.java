package org.duollectis.client.mixin.events.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.RenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.duollectis.client.utils.MC.mc;

/**
 * Mixin для {@link WorldRenderer}: публикует {@link RenderEvent} внутри синтетического
 * метода лямбды {@code framePass.setRenderer()} из {@code renderLateDebug} (Yarn: method_75413).
 * <p>
 * Инжект срабатывает перед {@code checkEmpty} — в этот момент GPU-пасс ещё активен.
 * {@code outputColorTextureOverride} уже обнулён кодом Minecraft, поэтому мы
 * выставляем его вручную из главного framebuffer, рисуем, и сбрасываем обратно в null.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

	@Inject(
			method = "method_75413",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V"
			)
	)
	private void duollectis$onRenderLateDebugLambda(CallbackInfo ci) {
		RenderSystem.outputColorTextureOverride = mc.getFramebuffer().getColorAttachmentView();
		RenderSystem.outputDepthTextureOverride = mc.getFramebuffer().getDepthAttachmentView();

		VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
		RenderEvent.INSTANCE.set(new MatrixStack(), (GameRenderer) mc.gameRenderer, immediate);
		EventBus.getInstance().post(RenderEvent.INSTANCE);
		immediate.draw();

		RenderSystem.outputColorTextureOverride = null;
		RenderSystem.outputDepthTextureOverride = null;
	}

}
