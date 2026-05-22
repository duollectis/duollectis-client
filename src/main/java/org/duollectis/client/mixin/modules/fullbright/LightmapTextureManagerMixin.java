package org.duollectis.client.mixin.modules.fullbright;

import net.minecraft.client.render.LightmapTextureManager;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.Fullbright;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

	/**
	 * Подменяет значение гаммы в UBO lightmap на максимальное (1.0),
	 * когда модуль Fullbright активен. Гамма — 7-й аргумент putFloat в цепочке
	 * Std140Builder.intoBuffer(...).putFloat(f).putFloat(g).putFloat(n)
	 * .putFloat(m).putFloat(k).putFloat(skyDarkness).putFloat(gamma <- ordinal=6)
	 */
	@ModifyArg(
		method = "update",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
			ordinal = 6
		)
	)
	private float duollectis$overrideGamma(float original) {
		Fullbright module = ModuleManager.getInstance().get(Fullbright.class);
		return module != null && module.isActive() ? module.getValue() : original;
	}
}
