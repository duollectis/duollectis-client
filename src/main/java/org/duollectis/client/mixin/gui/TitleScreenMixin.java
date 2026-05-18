package org.duollectis.client.mixin.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.menu.proxy.ProxyScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin() {
        super(Text.empty());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void duollectis$addProxyButton(CallbackInfo ci) {
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Proxy"),
                        btn -> client.setScreen(new ProxyScreen())
                )
                .dimensions(4, height - 24, 60, 20)
                .build()
        );
    }
}
