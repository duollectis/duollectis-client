package org.duollectis.client;

import net.fabricmc.api.ClientModInitializer;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.bind.BindManager;
import org.duollectis.client.api.clickgui.ClickGuiScreen;
import org.duollectis.client.api.i18n.ClientConfig;
import org.duollectis.client.api.menu.main.ClientScreen;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.api.proxy.ProxyManager;

import static org.duollectis.client.utils.MC.translations;

/** Точка входа клиентского мода. Инициализирует все подсистемы клиента. */
public class DuollectisMod implements ClientModInitializer {

	public static final String MOD_ID = "duollectis";

	@Override
	public void onInitializeClient() {
		translations().reload(ClientConfig.getInstance().getLocale());

		ProxyManager.getInstance().init();
		AccountManager.getInstance().init();
		ModuleManager.getInstance().init();

		BindManager bindManager = BindManager.getInstance();
		bindManager.registerSystemAction(BindManager.BIND_MENU, ClientScreen::open);
		bindManager.registerSystemAction(BindManager.BIND_CLICKGUI, ClickGuiScreen::open);

		Runtime.getRuntime().addShutdownHook(
			new Thread(() -> ModuleManager.getInstance().save(), "duollectis-save")
		);
	}
}
