package org.duollectis.client.modules;

import static org.duollectis.client.utils.MC.mc;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.utils.inventory.FindItemResult;
import org.duollectis.client.utils.inventory.InvUtils;
import org.duollectis.client.utils.network.PacketUtil;

@ModuleInfo("auto-gap")
/**
 * Модуль автоматического использования зачарованного золотого яблока (gap)
 * при падении здоровья ниже заданного порога. Поддерживает настраиваемую задержку
 * между применениями для предотвращения спама.
 */
public class AutoGap extends Module {

	private final Setting<Integer> minHealth = settings.add(IntSetting
			.create("min-health")
			.defaultValue(10)
			.min(0)
			.sliderRange(1, 36)
			.done());

	private final Setting<Integer> eatDelay = settings.add(IntSetting
			.create("eat-delay")
			.defaultValue(40)
			.min(0)
			.sliderRange(1, 100)
			.done());

	private int timer;

	@Override
	public void onDeactivate() {
		timer = 0;
	}

	@Subscribe
	private void onTickPre(TickPreEvent event) {
		if (mc.player == null) {
			return;
		}

		if (timer > 0) {
			timer--;
			return;
		}

		if (Math.round(mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= minHealth.get()) {
			FindItemResult result = InvUtils.findInHotbar(Items.ENCHANTED_GOLDEN_APPLE);

			if (result.found()) {
				timer = eatDelay.get();
				InvUtils.swap(result.slot(), true);
				PacketUtil.useItem(Hand.MAIN_HAND);
				InvUtils.swapBack();
				PacketUtil.stopUsingItem();
			}
		}
	}

}
