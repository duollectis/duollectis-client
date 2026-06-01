package org.duollectis.client.mixin.events.input;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.duollectis.client.api.event.EventBus;
import org.duollectis.client.api.event.events.input.MouseButtonEvent;
import org.duollectis.client.api.event.events.input.MouseMoveEvent;
import org.duollectis.client.api.event.events.input.MouseScrollEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

	@Inject(
		method = "onMouseButton",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
		if (action == GLFW.GLFW_PRESS) {
			MouseButtonEvent event = EventBus.getInstance()
				.post(MouseButtonEvent.INSTANCE.set(input.button(), input.modifiers()));

			if (event.isCancelled()) {
				ci.cancel();
			}
		}
	}

	@Inject(
		method = "onMouseScroll",
		at = @At("HEAD"),
		cancellable = true
	)
	private void duollectis$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		MouseScrollEvent event = EventBus.getInstance()
			.post(MouseScrollEvent.INSTANCE.set(horizontal, vertical));

		if (event.isCancelled()) {
			ci.cancel();
		}
	}

	@Inject(
		method = "onCursorPos",
		at = @At("HEAD")
	)
	private void duollectis$onCursorPos(long window, double newX, double newY, CallbackInfo ci) {
		EventBus.getInstance().post(MouseMoveEvent.INSTANCE.set(newX, newY));
	}
}
