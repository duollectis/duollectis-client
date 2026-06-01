package org.duollectis.client.mixin.events.input;

import net.minecraft.client.Keyboard;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.duollectis.client.api.bind.BindManager;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.input.CharTypeEvent;
import org.duollectis.client.api.event.events.input.KeyPressEvent;
import org.duollectis.client.api.event.events.input.KeyReleaseEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

	@Inject(
		method = "onKey",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
		if (action == GLFW.GLFW_PRESS) {
			KeyPressEvent event = EventBus.getInstance()
				.post(new KeyPressEvent().set(input.key(), input.scancode(), input.modifiers()));

			if (event.isCancelled()) {
				ci.cancel();
				return;
			}

			BindManager.getInstance().onKey(input.key());
		}
		else if (action == GLFW.GLFW_RELEASE) {
			EventBus.getInstance()
				.post(new KeyReleaseEvent().set(input.key(), input.scancode(), input.modifiers()));
		}
	}

	@Inject(
		method = "onChar",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$onChar(long window, CharInput input, CallbackInfo ci) {
		CharTypeEvent event = EventBus.getInstance()
			.post(new CharTypeEvent().set(input.codepoint(), input.modifiers()));

		if (event.isCancelled()) {
			ci.cancel();
		}
	}
}
