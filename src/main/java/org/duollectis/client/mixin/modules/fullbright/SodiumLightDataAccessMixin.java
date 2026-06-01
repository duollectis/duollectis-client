package org.duollectis.client.mixin.modules.fullbright;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.Fullbright;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = LightDataAccess.class, remap = false)
public abstract class SodiumLightDataAccessMixin {

	// packBL упаковывает block light в биты [0..3], packSL — в биты [4..7]
	private static final int BL_MASK = 0xF;
	private static final int SL_MASK = 0xF << 4;
	private static final int MAX_LIGHT = 15;

	@ModifyReturnValue(method = "compute", at = @At("RETURN"))
	private int overrideLightLevels(int packed) {
		Fullbright module = ModuleManager.getInstance().get(Fullbright.class);

		if (module == null || !module.isActive()) {
			return packed;
		}

		return (packed & ~BL_MASK & ~SL_MASK) | MAX_LIGHT | (MAX_LIGHT << 4);
	}

}
