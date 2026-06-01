package org.duollectis.client.utils;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.duollectis.client.api.i18n.ClientTranslations;

import java.io.File;
import java.nio.file.Path;

/**
 * Статические шорткаты для наиболее часто используемых объектов клиента и окружения.
 * Устраняет повторяющиеся вызовы {@code MinecraftClient.getInstance()},
 * {@code FabricLoader.getInstance().getGameDir()} и {@code ClientTranslations.getInstance()} по всему коду.
 * Использование: {@code import static org.duollectis.client.utils.MC.*;}
 */
@UtilityClass
public class MC {

	public static final MinecraftClient mc = MinecraftClient.getInstance();

	public static ClientPlayerEntity player() {
		return MinecraftClient.getInstance().player;
	}

	public static ClientWorld world() {
		return MinecraftClient.getInstance().world;
	}

	public static Path gameDir() {
		return FabricLoader.getInstance().getGameDir();
	}

	public static File gameDirFile() {
		return FabricLoader.getInstance().getGameDir().toFile();
	}

	public static ClientTranslations translations() {
		return ClientTranslations.getInstance();
	}

}
