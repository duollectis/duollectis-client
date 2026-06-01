package org.duollectis.client.modules;

import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;

@ModuleInfo(
		value = "fullbright",
		category = Category.RENDER
)
public class Fullbright extends Module {

	@Override
	public void onDeactivate() {
		if (mc.worldRenderer != null) {
			mc.worldRenderer.reload();
		}
	}

}
