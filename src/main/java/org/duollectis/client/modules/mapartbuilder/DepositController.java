package org.duollectis.client.modules.mapartbuilder;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.PacketReceiveEvent;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Controller;
import org.duollectis.client.api.tick.Cooldown;
import org.duollectis.client.api.tick.OpenWatchdog;
import org.duollectis.client.utils.network.PacketUtil;
import org.duollectis.client.utils.network.ScreenHandlerContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Открывает контейнер и перекладывает в него лишние блоки из инвентаря игрока через QUICK_MOVE.
 * «Лишними» считаются предметы, количество которых в инвентаре превышает целевое значение из {@code toKeep}.
 * Предметы, отсутствующие в {@code toKeep}, не трогаются.
 */
class DepositController extends Controller<MapArtBuilder> {

	private static final int PLAYER_INVENTORY_SIZE = 36;
	private static final int MAX_OPEN_ATTEMPTS = 3;
	private static final int DEPOSIT_COOLDOWN_TICKS = 5;
	private static final int NO_PROGRESS = -1;

	private final ScreenHandlerContext context = new ScreenHandlerContext();
	private final Cooldown cooldown = new Cooldown();

	private Map<Item, Integer> toKeep;

	@Nullable
	private BlockPos openPos;

	private OpenWatchdog watchdog;
	private int containerSize;
	// Суммарное количество лишних предметов до последней попытки депозита.
	// Если после кулдауна оно не изменилось — шалкер полон, завершаем.
	private int excessBeforeDeposit;

	private boolean opened;
	private boolean done;

	@Override
	protected void onDeactivate() {
		reset();
	}

	/**
	 * Запускает сдачу лишних предметов в контейнер по заданной позиции.
	 *
	 * @param pos    позиция контейнера
	 * @param toKeep карта «предмет → сколько единиц оставить в инвентаре»
	 */
	void start(BlockPos pos, Map<Item, Integer> toKeep) {
		reset();
		this.openPos = pos;
		this.toKeep = toKeep;
		this.watchdog = new OpenWatchdog(MAX_OPEN_ATTEMPTS, module.openTimeout.get().intValue());

		sendOpenPacket(pos);
		opened = true;
	}

	void reset() {
		context.close();
		openPos = null;
		toKeep = null;
		watchdog = null;
		containerSize = 0;
		excessBeforeDeposit = NO_PROGRESS;
		cooldown.reset();
		opened = false;
		done = false;
	}

	boolean isOpened() {
		return opened;
	}

	boolean isDone() {
		return done;
	}

	@Subscribe
	private void onTick(TickPreEvent event) {
		if (done || !opened) {
			return;
		}

		if (!context.isOpen()) {
			watchdog.tick(
				() -> {
					debugInfo("[Deposit] Ватчдог: шалкер не открылся, попытка "
						+ watchdog.attempts() + "/" + watchdog.maxAttempts() + " для " + openPos);
					sendOpenPacket(openPos);
				},
				() -> {
					debugInfo("[Deposit] Ватчдог: превышен лимит попыток ("
						+ MAX_OPEN_ATTEMPTS + ") — пропускаем хранилище " + openPos);
					context.close();
					opened = false;
					done = true;
				}
			);

			return;
		}

		watchdog.reset();
		cooldown.tick();

		if (cooldown.isActive()) {
			return;
		}

		depositExcess();
	}

	@Subscribe
	private void onPacket(PacketReceiveEvent event) {
		if (done || !opened) {
			return;
		}

		boolean handled = context.handlePacket(event.getPacket());

		if (!handled) {
			return;
		}

		event.cancel();

		if (event.getPacket() instanceof InventoryS2CPacket) {
			containerSize = context.getScreenHandler().slots.size() - PLAYER_INVENTORY_SIZE;
			debugInfo("[Deposit] Контейнер открыт, слотов=" + containerSize + ", syncId=" + context.getScreenHandler().syncId);
		}
	}

	/**
	 * За один тик перекладывает все лишние предметы из инвентаря игрока в контейнер.
	 * Слоты контейнера (0..containerSize-1) пропускаются — кликаем только по слотам игрока.
	 * QUICK_MOVE из слота игрока при открытом контейнере перемещает стак в контейнер.
	 * <p>
	 * Если после кулдауна суммарный излишек не уменьшился — шалкер полон, завершаем депозит.
	 */
	private void depositExcess() {
		DefaultedList<Slot> slots = context.getScreenHandler().slots;
		int currentExcess = countTotalExcess(slots);

		if (currentExcess == 0) {
			finishDeposit();
			return;
		}

		// Шалкер полон: после предыдущей попытки ничего не изменилось
		if (excessBeforeDeposit != NO_PROGRESS && currentExcess >= excessBeforeDeposit) {
			debugInfo("[Deposit] Шалкер полон (излишек не уменьшился: " + currentExcess + "), завершаем");
			finishDeposit();
			return;
		}

		excessBeforeDeposit = currentExcess;

		for (int i = containerSize; i < slots.size(); i++) {
			ItemStack stack = slots.get(i).getStack();

			if (stack.isEmpty()) {
				continue;
			}

			Item item = stack.getItem();

			if (!(item instanceof BlockItem)) {
				continue;
			}

			Integer keepCount = toKeep.get(item);

			if (keepCount == null) {
				continue;
			}

			int inInventory = countItemInPlayerSlots(slots, item);

			if (inInventory <= keepCount) {
				continue;
			}

			debugInfo("[Deposit] Сдаём лишнее: " + item + " в инвентаре=" + inInventory + " оставить=" + keepCount + " слот=" + i);
			context.clickSlot(i, 0, SlotActionType.QUICK_MOVE);
		}

		cooldown.set(DEPOSIT_COOLDOWN_TICKS);
	}

	/**
	 * Считает суммарный излишек всех предметов из {@code toKeep} в слотах игрока.
	 */
	private int countTotalExcess(DefaultedList<Slot> slots) {
		int total = 0;

		for (Map.Entry<Item, Integer> entry : toKeep.entrySet()) {
			Item item = entry.getKey();
			int keepCount = entry.getValue();
			int inInventory = countItemInPlayerSlots(slots, item);
			int excess = inInventory - keepCount;

			if (excess > 0) {
				total += excess;
			}
		}

		return total;
	}

	private void finishDeposit() {
		debugInfo("[Deposit] Завершено, syncId=" + context.getScreenHandler().syncId);
		PacketUtil.sendPacket(new CloseHandledScreenC2SPacket(context.getScreenHandler().syncId));
		context.close();
		opened = false;
		done = true;
	}

	private void sendOpenPacket(BlockPos pos) {
		mc.interactionManager.interactBlock(
			mc.player,
			Hand.MAIN_HAND,
			new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false)
		);
	}

	/**
	 * Считает суммарное количество предмета в слотах игрока (часть slots начиная с containerSize).
	 */
	private int countItemInPlayerSlots(DefaultedList<Slot> slots, Item item) {
		int count = 0;

		for (int i = containerSize; i < slots.size(); i++) {
			ItemStack stack = slots.get(i).getStack();

			if (stack.isOf(item)) {
				count += stack.getCount();
			}
		}

		return count;
	}
}
