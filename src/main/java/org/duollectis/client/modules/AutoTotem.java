package org.duollectis.client.modules;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.utils.inventory.SlotUtils;

@ModuleInfo("auto-totem")
public class AutoTotem extends Module {

	/**
	 * Индекс оффхенда в протоколе Minecraft: используется как data в SlotActionType.SWAP
	 * для эмуляции нажатия клавиши F (swap with offhand) на слоте инвентаря.
	 */
	private static final int OFFHAND_HOTBAR_INDEX = 40;
	private static final int INVENTORY_SIZE = 36;

	private final Setting<Integer> swapDelay = settings.add(IntSetting
		.create("swap-delay")
		.defaultValue(1)
		.min(0)
		.sliderRange(0, 20)
		.done()
	);

	private int timer;

	@Override
	public void onDeactivate() {
		timer = 0;
	}

	@Subscribe(TickPreEvent.class)
	private void onTickPre() {
		if (mc.player == null) {
			return;
		}

		if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
			timer = 0;
			return;
		}

		if (timer > 0) {
			timer--;
			return;
		}

		timer = swapDelay.get();

		// Отправляем swap-пакет для каждого тотема в инвентаре подряд.
		// При высоком пинге сервер применит первый валидный, остальные проигнорирует.
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);

			if (stack.getItem() != Items.TOTEM_OF_UNDYING) {
				continue;
			}

			int networkSlotId = SlotUtils.indexToId(i);

			if (networkSlotId == -1) {
				continue;
			}

			mc.interactionManager.clickSlot(
				mc.player.currentScreenHandler.syncId,
				networkSlotId,
				OFFHAND_HOTBAR_INDEX,
				SlotActionType.SWAP,
				mc.player
			);
		}
	}

}
