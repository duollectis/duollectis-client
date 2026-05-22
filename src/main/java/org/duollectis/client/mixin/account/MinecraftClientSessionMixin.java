package org.duollectis.client.mixin.account;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mixin-аксессор для {@link MinecraftClient}: открывает доступ на запись
 * к приватному финальному полю {@code session}, что позволяет подменять
 * игровую сессию (ник/UUID) без перезапуска клиента.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientSessionMixin {

	@Mutable
	@Accessor("session")
	void duollectis$setSession(Session session);

	@Accessor("session")
	Session duollectis$getSession();
}
