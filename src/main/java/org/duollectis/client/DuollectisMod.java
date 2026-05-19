package org.duollectis.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.event.events.TickPostEvent;
import org.duollectis.client.api.i18n.ClientConfig;
import org.duollectis.client.api.i18n.ClientTranslations;
import org.duollectis.client.api.menu.modules.ModulesScreen;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.api.proxy.ProxyManager;
import org.lwjgl.glfw.GLFW;

public class DuollectisMod implements ClientModInitializer {

    public static final String MOD_ID = "duollectis";

    @Override
    public void onInitializeClient() {
        ClientTranslations.getInstance().reload(ClientConfig.getInstance().getLocale());

        new ProxyManager();

        ModuleManager.getInstance();

        KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.duollectis.menu.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyBinding.Category.create(Identifier.of("duollectis", "category"))
        ));

        boolean[] menuKeyHeld = {false};

        EventBus.getInstance().subscribe(TickPostEvent.class, event -> {
            boolean pressed = InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow(),
                GLFW.GLFW_KEY_RIGHT_SHIFT
            );

            if (pressed && menuKeyHeld[0] == false) {
                menuKeyHeld[0] = true;
                ModulesScreen.open();
            } else if (pressed == false) {
                menuKeyHeld[0] = false;
            }
        });

        WorldRenderEvents.END_MAIN.register(context ->
            EventBus.getInstance().post(new RenderEvent(
                context.matrices(),
                context.gameRenderer(),
                context.consumers()
            ))
        );
    }

}
