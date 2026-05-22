package org.duollectis.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.util.InputUtil;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.TickPostEvent;
import org.duollectis.client.api.i18n.ClientConfig;
import org.duollectis.client.api.i18n.ClientTranslations;
import org.duollectis.client.api.menu.modules.ModulesScreen;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.api.proxy.ProxyManager;
import org.lwjgl.glfw.GLFW;

import static org.duollectis.client.utils.MC.mc;

/** Точка входа клиентского мода. Инициализирует подсистемы и подписывается на тик для обработки горячих клавиш. */
public class DuollectisMod implements ClientModInitializer {

	public static final String MOD_ID = "duollectis";

	@Override
	public void onInitializeClient() {
		ClientTranslations.getInstance().reload(ClientConfig.getInstance().getLocale());

		new ProxyManager();
		new AccountManager();
		ModuleManager.getInstance();

		boolean[] menuKeyHeld = {false};
		boolean[] moduleKeyHeld = new boolean[GLFW.GLFW_KEY_LAST + 1];

		EventBus.getInstance().subscribe(TickPostEvent.class, event -> {
			boolean menuPressed = InputUtil.isKeyPressed(
				mc.getWindow(),
				GLFW.GLFW_KEY_RIGHT_SHIFT
			);

			if (menuPressed && menuKeyHeld[0]) {
				return;
			}

			if (menuPressed) {
				menuKeyHeld[0] = true;
				ModulesScreen.open();
				return;
			}

			menuKeyHeld[0] = false;

			if (mc.currentScreen != null) {
				return;
			}

			for (Module module : ModuleManager.getInstance().all()) {
				int code = module.getKeyCode();

				if (code < 0 || code >= moduleKeyHeld.length) {
					continue;
				}

				boolean keyPressed = InputUtil.isKeyPressed(mc.getWindow(), code);

				if (keyPressed && moduleKeyHeld[code]) {
					continue;
				}

				moduleKeyHeld[code] = keyPressed;

				if (keyPressed) {
					module.toggle();
				}
			}
		});
	}

}
