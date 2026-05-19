package org.duollectis.client.modules;

import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.utils.render.RenderUtils;

@ModuleInfo("test")
public class TestModule extends Module {

	@Subscribe
	private void onRender(RenderEvent event) {
		if (mc.player == null) {
			return;
		}

		RenderUtils.drawLinesBox(event.getMatrices(), mc.player.getBlockPos(), 0xFFFFFF00);
	}

}
