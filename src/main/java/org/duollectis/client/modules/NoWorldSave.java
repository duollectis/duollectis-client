package org.duollectis.client.modules;

import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;

@ModuleInfo(
		value = "no-world-save",
		category = Category.MISC
)
public class NoWorldSave extends Module {

	private static volatile boolean blockSave;

	public static boolean isBlockingSave() {
		return blockSave;
	}

	@Override
	protected void onActivate() {
		blockSave = true;
	}

	@Override
	protected void onDeactivate() {
		blockSave = false;
	}

}
