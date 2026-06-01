package org.duollectis.client.mixin.modules.chams;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.duollectis.client.modules.Chams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Добавляет два визуальных эффекта поверх оригинальной модели:
 *
 * <p>1. Шейдерный проход (FLAT/RAINBOW/PULSE) — кастомный RenderLayer с NO_DEPTH_TEST.
 * Цвет передаётся через tintedColor. Режим NONE — шейдер не рендерится.
 *
 * <p>2. Ванильный 2D-силуэт — передаётся через outlineColor в submitModel.
 * RenderLayer помечен как AFFECTS_OUTLINE, что активирует entityOutlineFramebuffer
 * и post-processor entity_outline — настоящий плоский силуэт с контуром по краям.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererChamsMixin<
	T extends LivingEntity,
	S extends LivingEntityRenderState,
	M extends EntityModel<? super S>
> {

	@SuppressWarnings("unchecked")
	@Inject(
		method = "render",
		at = @At("TAIL")
	)
	private void duollectis$renderChams(
		S state,
		MatrixStack matrices,
		OrderedRenderCommandQueue queue,
		CameraRenderState cameraState,
		CallbackInfo ci
	) {
		Chams chams = Chams.getInstance();

		if (chams == null || !chams.isActive()) {
			return;
		}

		LivingEntityRenderer<T, S, M> self = (LivingEntityRenderer<T, S, M>) (Object) this;
		RenderLayer shaderLayer = chams.getShaderLayer(self.getTexture(state));

		if (shaderLayer == null) {
			return;
		}

		int outlineArgb = chams.isOutlineEnabled() ? chams.getOutlineColorPacked() : 0;

		queue.submitModel(
			self.getModel(),
			state,
			matrices,
			shaderLayer,
			state.light,
			OverlayTexture.DEFAULT_UV,
			chams.getShaderColorPacked(),
			null,
			outlineArgb,
			null
		);
	}

}
