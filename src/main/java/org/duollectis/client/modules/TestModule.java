package org.duollectis.client.modules;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.utils.RenderUtils;

@ModuleInfo(
		name = "test",
		description = "TEST"
)
public class TestModule extends Module {

	@Override
	public void onRender(MatrixStack matrices, GameRenderer gameRenderer, VertexConsumerProvider consumers) {
		RenderUtils.drawLinesBox(matrices, mc.player.getBlockPos(), 0xFFFFFF00);
	}
}
