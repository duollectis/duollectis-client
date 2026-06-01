package org.duollectis.client.modules;

import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.DoubleSetting;

/**
 * Модуль для сингл-плеера: заставляет всех зомби всегда подбирать предметы,
 * а при убийстве именованного зомби игроком — дюпает поднятый предмет с шансом 23%.
 */
@ModuleInfo(
		value = "zombie-item-pickup",
		category = Category.MISC
)
public class ZombieItemPickup extends Module {

	private final Setting<Double> chance = settings.add(DoubleSetting
			.create("chance")
			.defaultValue(1D)
			.range(0, 1)
			.done());

	public double getChance() {
		return chance.get();
	}
}
