package org.duollectis.client;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.duollectis.client.api.event.EventHandler;
import org.duollectis.client.api.event.EventManager;
import org.duollectis.client.api.menu.ModulesMenu;
import org.duollectis.client.api.modules.ModuleManager;
import org.lwjgl.glfw.GLFW;

public class DuollectisMod implements ClientModInitializer {

	public static final String MOD_ID = "duollectis";

	@Getter
	private static final EventManager<EventHandler> eventManager = new EventManager<>();

	@Override
	public void onInitializeClient() {
		eventManager.add(ModuleManager.getInstance());

		KeyBinding menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.duollectis.menu.open",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KeyBinding.Category.create(Identifier.of("duollectis", "category"))
		));

		eventManager.add(new EventHandler() {
			@Override
			public void onTickPost() {
				while (menuKey.wasPressed()) {
					ModulesMenu.open();
				}
			}
		});

		WorldRenderEvents.END_MAIN.register(context -> eventManager.onRender(
				context.matrices(),
				context.gameRenderer(),
				context.consumers()
		));

//		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
//
//		});
	}
}