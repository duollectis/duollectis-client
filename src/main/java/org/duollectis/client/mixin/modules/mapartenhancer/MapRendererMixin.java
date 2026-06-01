package org.duollectis.client.mixin.modules.mapartenhancer;

import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.render.entity.state.ItemFrameEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.MapArtEnhancer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public class MapRendererMixin {

	/**
	 * Перед вызовом mapRenderer.draw() масштабирует матрицу так, чтобы карта
	 * выходила за края рамки на ~0.5px с каждой стороны.
	 * Это перекрывает физический зазор между соседними рамками без z-fighting,
	 * поскольку карта уже отодвинута от геометрии рамки по Z на -1px (в пространстве 128px).
	 */
	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/MapRenderer;draw(Lnet/minecraft/client/render/MapRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ZI)V"
		)
	)
	private void duollectis$expandMapQuad(
		ItemFrameEntityRenderState state,
		MatrixStack matrices,
		OrderedRenderCommandQueue queue,
		CameraRenderState cameraState,
		CallbackInfo ci
	) {
		MapArtEnhancer module = ModuleManager.getInstance().get(MapArtEnhancer.class);

		if (module == null || !module.isActive()) {
			return;
		}

		// В этой точке матрица уже в пространстве 128px (scale = 1/128).
		// Расширяем карту на 0.5px с каждой стороны: сначала сдвигаем центр,
		// затем масштабируем. 129/128 ≈ 1.0078 — карта выходит за края рамки
		// ровно на половину пикселя карты (~0.004 блока) с каждой стороны.
		float seamScale = 129.0F / 128.0F;

		matrices.translate(-0.5F, -0.5F, 0.0F);
		matrices.scale(seamScale, seamScale, 1.0F);
	}

}
