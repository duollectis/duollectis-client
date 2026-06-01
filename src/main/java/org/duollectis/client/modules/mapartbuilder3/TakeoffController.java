package org.duollectis.client.modules.mapartbuilder3;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.PacketReceiveEvent;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Controller;
import org.duollectis.client.api.tick.Cooldown;
import org.duollectis.client.modules.mapartbuilder.MapArtBuilder;
import org.duollectis.client.utils.inventory.InvUtils;
import org.duollectis.client.utils.inventory.SlotUtils;
import org.duollectis.client.utils.network.PacketUtil;

/**
 * Контроллер автоматического взлёта на элитрах.
 */
class TakeoffController extends Controller<MapArtBuilder3> {

	private static final int ENTITY_FLAGS_TRACKER_ID = 0;
	private static final byte FALL_FLYING_BIT = (byte) 0x80;
	private static final int INVENTORY_SIZE = 36;
	// Сетевой ID слота нагрудника в PlayerScreenHandler (survival inventory)
	private static final int CHEST_SCREEN_SLOT_ID = 6;
	// Первый слот основного инвентаря (не хотбар)
	private static final int MAIN_INVENTORY_START = 9;

	private static final int ELYTRA_SWAP_COOLDOWN_TICKS = 10;
	private static final int GLIDE_CONFIRM_TIMEOUT_TICKS = 20;

	private final Cooldown elytraSwapCooldown = new Cooldown();
	private final Cooldown waitingGlideCooldown = new Cooldown();
	private boolean jumped;
	private double lastY;

	@Override
	public void onDeactivate() {
		elytraSwapCooldown.reset();
		waitingGlideCooldown.reset();
		jumped = false;
		lastY = 0;
	}

	@Subscribe
	private void onPacket(PacketReceiveEvent event) {
		handleFlyConfirm(event);
	}

	@Subscribe(TickPreEvent.class)
	private void onTick() {
		changeElytra();
		tryStartGliding();
		waitingGlideCooldown.tick();
	}

	/**
	 * Проверяет прочность надетой элитры и, если она опустилась ниже порога из настройки,
	 * ищет в инвентаре свежую элитру и надевает её через тройной хотбар-свап.
	 */
	private void changeElytra() {
		if (mc.player == null) {
			return;
		}

		elytraSwapCooldown.tick();

		if (elytraSwapCooldown.isActive()) {
			return;
		}

		if (mc.player.currentScreenHandler != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
			mc.player.closeHandledScreen();
			return;
		}

		ItemStack equipped = mc.player.getEquippedStack(EquipmentSlot.CHEST);
		double threshold = module.elytraSwapThreshold.get();

		boolean needsElytra = equipped.isEmpty()
				|| !equipped.isOf(Items.ELYTRA)
				|| durabilityPercent(equipped) <= threshold;

		if (!needsElytra) {
			return;
		}

		int elytraSlot = findFreshElytraSlot(threshold);

		if (elytraSlot == -1) {
			info("[Элитра] Нужна замена, но свежей элитры в инвентаре нет (слоты 9–35)");
			return;
		}

		int hotbarSlot = mc.player.getInventory().getSelectedSlot();
		int elytraNetId = SlotUtils.indexToId(elytraSlot);

		info(
				"[Элитра] Свап: нагрудник=%s прочность=%.0f%% | инв.слот=%d netId=%d хотбар=%d"
						.formatted(
								equipped.isEmpty() ? "пусто" : equipped.getItem().toString(),
								equipped.isEmpty() ? 0.0 : durabilityPercent(equipped),
								elytraSlot,
								elytraNetId,
								hotbarSlot
						)
		);

		// Шаг 1: элитра из инвентаря ↔ предмет из текущего хотбар-слота
		InvUtils.swap()
		        .fromId(hotbarSlot)
		        .toId(elytraNetId);

		// Шаг 2: хотбар (теперь элитра) ↔ нагрудник
		InvUtils.swap()
		        .fromId(hotbarSlot)
		        .toId(CHEST_SCREEN_SLOT_ID);

		// Шаг 3: возвращаем вытесненный предмет обратно в инвентарный слот
		InvUtils.swap()
		        .fromId(hotbarSlot)
		        .toId(elytraNetId);

		elytraSwapCooldown.set(ELYTRA_SWAP_COOLDOWN_TICKS);
	}

	/**
	 * Слушает входящие пакеты трекера сущностей и подтверждает начало планирования,
	 * когда сервер выставляет бит {@code FALL_FLYING} (0x80) в флагах игрока.
	 * Сбрасывает таймер ожидания, разрешая контроллеру перейти к управлению полётом.
	 */
	private void handleFlyConfirm(PacketReceiveEvent event) {
		if (waitingGlideCooldown.isReady()) {
			return;
		}

		if (!(event.getPacket() instanceof EntityTrackerUpdateS2CPacket packet)) {
			return;
		}

		assert mc.player != null;
		if (packet.id() != mc.player.getId()) {
			return;
		}

		for (DataTracker.SerializedEntry<?> entry : packet.trackedValues()) {
			if (entry.id() != ENTITY_FLAGS_TRACKER_ID) {
				continue;
			}

			if (entry.handler() != TrackedDataHandlerRegistry.BYTE) {
				continue;
			}

			byte flags = (Byte) entry.value();

			if ((flags & FALL_FLYING_BIT) == 0) {
				continue;
			}

			waitingGlideCooldown.reset();
			debugInfo("[Этап 1] Взлёт подтверждён сервером — начинаем полёт");
			return;
		}
	}

	/**
	 * Каждый тик проверяет, нужно ли инициировать взлёт.
	 * Пропускает попытку, если игрок уже планирует, таймер ещё не истёк или нет элитры.
	 */
	private void tryStartGliding() {
		assert mc.player != null;
		if (mc.player.isGliding() || waitingGlideCooldown.isActive() || !hasElytraEquipped()) {
			return;
		}

		if (mc.player.isOnGround() && !jumped) {
			debugInfo("GLIDE JUMPING");
			mc.player.jump();
			jumped = true;
			return;
		}

		if (jumped && lastY < mc.player.getY()) {
			debugInfo("GLIDE WAIT DOWN");
			lastY = mc.player.getY();
			return;
		}

		jumped = false;

		debugInfo("START GLIDING");
		PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
		waitingGlideCooldown.set(GLIDE_CONFIRM_TIMEOUT_TICKS);
	}

	private static boolean hasElytraEquipped() {
		assert mc.player != null;
		return mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
	}

	/**
	 * Ищет в основном инвентаре (слоты 9–35, без хотбара) элитру с прочностью выше порога.
	 * Хотбар исключён намеренно: для Шага 1 свапа нужен слот из основного инвентаря,
	 * иначе {@link SlotUtils#indexToId} вернёт неверный сетевой ID для хотбар-слота.
	 * Среди кандидатов выбирает ту, у которой прочность максимальна.
	 *
	 * @param threshold минимальный порог прочности в процентах (не включительно)
	 * @return индекс слота лучшей элитры, или {@code -1} если не найдено
	 */
	private static int findFreshElytraSlot(double threshold) {
		assert mc.player != null;
		int bestSlot = -1;
		double bestDurability = -1;

		for (int i = MAIN_INVENTORY_START; i < INVENTORY_SIZE; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);

			if (stack.isEmpty() || !stack.isOf(Items.ELYTRA)) {
				continue;
			}

			double durability = durabilityPercent(stack);

			if (durability <= threshold || durability <= bestDurability) {
				continue;
			}

			bestSlot = i;
			bestDurability = durability;
		}

		return bestSlot;
	}

	private static double durabilityPercent(ItemStack stack) {
		var damage = stack.get(DataComponentTypes.DAMAGE);
		var maxDamage = stack.get(DataComponentTypes.MAX_DAMAGE);

		if (damage == null || maxDamage == null || maxDamage == 0) {
			return 100.0;
		}

		return (double) (maxDamage - damage) / maxDamage * 100.0;
	}
}
