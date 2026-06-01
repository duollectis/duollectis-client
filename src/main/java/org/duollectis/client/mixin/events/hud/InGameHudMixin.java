package org.duollectis.client.mixin.events.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.HudRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void duollectis$onHudRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		HudRenderEvent.INSTANCE.set(context, tickCounter);
		EventBus.getInstance().post(HudRenderEvent.INSTANCE);
	}

}
