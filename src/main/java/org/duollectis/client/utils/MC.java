package org.duollectis.client.utils;

import net.minecraft.client.MinecraftClient;

/**
 * Публичные константы для статического импорта.
 * Устраняет повторяющиеся вызовы {@code MinecraftClient.getInstance()} по всему коду.
 * Использование: {@code import static org.duollectis.client.utils.MC.mc;}
 */
public final class MC {

	/** Синглтон игрового клиента. */
	public static final MinecraftClient mc = MinecraftClient.getInstance();

	private MC() {}
}
