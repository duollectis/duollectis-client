package org.duollectis.client.mixin.modules.mapartenhancer;

import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractTexture.class)
public interface NativeImageBackedTextureMixin {

	@Accessor("sampler")
	void setSampler(GpuSampler sampler);

}
