package org.duollectis.client.mixin.bind;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.duollectis.client.api.bind.BindManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Инжектирует системные бинды клиента (меню, ClickGui) в массив {@code GameOptions.allKeys}.
 * Это позволяет им отображаться в стандартном экране настроек управления Minecraft
 * без использования Fabric API — только через миксин.
 *
 * <p>Поле {@code allKeys} — {@code public final}, поэтому для его расширения
 * используется рефлексия с принудительным снятием {@code final}-модификатора.
 * Инжект происходит в конце конструктора {@link GameOptions}, когда массив уже собран.
 */
@Mixin(GameOptions.class)
public abstract class GameOptionsKeysMixin {

	@Shadow
	public KeyBinding[] allKeys;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void duollectis$injectClientKeys(CallbackInfo ci) {
		KeyBinding[] extended = Arrays.copyOf(allKeys, allKeys.length + 2);
		extended[allKeys.length] = BindManager.KEY_MENU;
		extended[allKeys.length + 1] = BindManager.KEY_CLICKGUI;

		try {
			Field field = GameOptions.class.getDeclaredField("allKeys");
			field.setAccessible(true);
			field.set(this, extended);
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException("Не удалось инжектировать клиентские бинды в GameOptions.allKeys", e);
		}
	}
}
