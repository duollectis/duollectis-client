package org.duollectis.client.mixin.modules.freecam;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.modules.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Блокирует ввод управления персонажем пока активен {@link FreeCam}.
 * Целевой класс — {@link KeyboardInput}, так как именно он переопределяет
 * {@code tick()} и заполняет поля реальными данными с клавиатуры.
 * Поля {@code playerInput} и {@code movementVector} объявлены в суперклассе
 * {@link Input}, поэтому доступ к ним осуществляется через явный каст.
 */
@Mixin(KeyboardInput.class)
public class FreeCamPlayerMixin {

	@Inject(
		method = "tick",
		at = @At("TAIL")
	)
	private void duollectis$blockInput(CallbackInfo ci) {
		FreeCam module = ModuleManager.getInstance().get(FreeCam.class);

		if (module == null || !module.isActive()) {
			return;
		}

		Input input = (Input) (Object) this;
		input.playerInput = PlayerInput.DEFAULT;
		input.movementVector = Vec2f.ZERO;
	}

}
