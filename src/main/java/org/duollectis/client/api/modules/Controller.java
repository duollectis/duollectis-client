package org.duollectis.client.api.modules;

import net.minecraft.client.MinecraftClient;
import org.duollectis.client.api.ILoggable;

public abstract class Controller<T extends Module> extends ControllerNode implements ILoggable {

	protected static final MinecraftClient mc = MinecraftClient.getInstance();

	protected T module;

	protected void onInit() {}

	protected void addController(Controller<T> sub) {
		addController(module, sub);
	}

	@Override
	public void info(Object arg) {
		if (module != null) {
			module.info(arg);
		}
	}

	@Override
	public void warning(Object arg) {
		if (module != null) {
			module.warning(arg);
		}
	}

	@Override
	public void error(Object arg) {
		if (module != null) {
			module.error(arg);
		}
	}

	@Override
	public void debugInfo(Object arg) {
		if (module != null) {
			module.debugInfo(arg);
		}
	}
}
