package org.duollectis.client.mixin.events.render;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin-заглушка для {@link GameRenderer}.
 * Публикация {@code RenderEvent} перенесена в {@code WorldRendererMixin},
 * где world-проекция гарантированно ещё активна.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

}
