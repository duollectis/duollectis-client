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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Открывает контейнер без GUI, перехватывает пакеты через {@link ScreenHandlerContext}.
 * Каждые {@code cooldownTicks} тиков берёт до {@code clicksPerBatch} нужных слотов,
 * пока не заберёт всё необходимое или инвентарь не заполнится.
 */
class CollectController extends Controller<MapArtBuilder> {

	private static final int PLAYER_INVENTORY_SIZE = 36;
	private static final int MAX_OPEN_ATTEMPTS = 15;

	private final ScreenHandlerContext context = new ScreenHandlerContext();
	private final Cooldown cooldown = new Cooldown();

	private Map<Item, Integer> needed;
	private List<CachedBlock> remainingBlocks;

	@Nullable
	private BlockPos openPos;

	private OpenWatchdog watchdog;
	private int clicksPerBatch;
	private int cooldownTicks;
	private int currentSlotIndex;
	private int containerSize;

	private boolean opened;
	private boolean done;

	@Override
	protected void onDeactivate() {
		reset();
	}

	/**
	 * Запускает сбор из контейнера по заданной позиции.
	 *
	 * @param pos            позиция контейнера
	 * @param needed         карта «предмет → нужное количество единиц в инвентаре»
	 * @param clicksPerBatch количество кликов за одну итерацию
	 * @param cooldownTicks  пауза в тиках между итерациями
	 */
	void start(BlockPos pos, Map<Item, Integer> needed, int clicksPerBatch, int cooldownTicks) {
		reset();
		this.openPos = pos;
		this.needed = needed;
		this.clicksPerBatch = clicksPerBatch;
		this.cooldownTicks = cooldownTicks;
		this.watchdog = new OpenWatchdog(MAX_OPEN_ATTEMPTS, module.openTimeout.get().intValue());

		sendOpenPacket(pos);
		opened = true;
	}

	void reset() {
		context.close();
		openPos = null;
		needed = null;
		remainingBlocks = null;
		watchdog = null;
		clicksPerBatch = 0;
		cooldownTicks = 0;
		currentSlotIndex = 0;
		containerSize = 0;
		cooldown.reset();
		opened = false;
		done = false;
	}

	List<CachedBlock> getRemainingBlocks() {
		return remainingBlocks == null ? List.of() : remainingBlocks;
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
			boolean gaveUp = watchdog.tick(
				() -> {
					debugInfo("[Collect] Ватчдог: шалкер не открылся, попытка "
						+ watchdog.attempts() + "/" + watchdog.maxAttempts() + " для " + openPos);
					sendOpenPacket(openPos);
				},
				() -> {
					debugInfo("[Collect] Ватчдог: превышен лимит попыток ("
						+ MAX_OPEN_ATTEMPTS + ") — пропускаем хранилище " + openPos);
					remainingBlocks = List.of();
					context.close();
					opened = false;
					done = true;
				}
			);

			if (gaveUp) {
				return;
			}

			return;
		}

		watchdog.reset();
		cooldown.tick();

		if (cooldown.isActive()) {
			return;
		}

		if (countEmptySlotsInInventory() == 0) {
			finishCollect();
			return;
		}

		if (!hasAnythingToCollect()) {
			finishCollect();
			return;
		}

		int clicked = collectBatch();

		if (clicked == 0) {
			finishCollect();
			return;
		}

		cooldown.set(cooldownTicks);
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
			remainingBlocks = buildRemainingBlocks(context.getScreenHandler().slots, containerSize);
			debugInfo("[Collect] Контейнер открыт, слотов=" + containerSize + ", syncId=" + context.getScreenHandler().syncId);
		}
	}

	/**
	 * Выполняет до {@code clicksPerBatch} кликов начиная с {@code currentSlotIndex},
	 * пропуская слоты без нужных предметов.
	 *
	 * @return количество фактически отправленных кликов
	 */
	private int collectBatch() {
		DefaultedList<Slot> slots = context.getScreenHandler().slots;
		Map<Item, Integer> clickedThisBatch = new HashMap<>();
		int clicked = 0;

		while (currentSlotIndex < containerSize && clicked < clicksPerBatch) {
			ItemStack stack = slots.get(currentSlotIndex).getStack();

			if (stack.isEmpty()) {
				currentSlotIndex++;
				continue;
			}

			Item item = stack.getItem();
			int alreadyClicked = clickedThisBatch.getOrDefault(item, 0);
			int allowed = allowedClicksFor(item, stack) - alreadyClicked;

			if (allowed <= 0) {
				currentSlotIndex++;
				continue;
			}

			context.clickSlot(currentSlotIndex, 0, SlotActionType.QUICK_MOVE);
			clickedThisBatch.merge(item, 1, Integer::sum);
			clicked++;
			currentSlotIndex++;
		}

		return clicked;
	}

	/**
	 * Проверяет, есть ли в контейнере хотя бы один слот с нужным предметом,
	 * которого ещё не хватает в инвентаре (с учётом уже собранного).
	 */
	private boolean hasAnythingToCollect() {
		DefaultedList<Slot> slots = context.getScreenHandler().slots;

		for (int i = currentSlotIndex; i < containerSize; i++) {
			ItemStack stack = slots.get(i).getStack();

			if (stack.isEmpty()) {
				continue;
			}

			if (allowedClicksFor(stack.getItem(), stack) > 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Вычисляет, сколько стаков данного предмета ещё можно взять из контейнера.
	 * {@code needed} хранит количество единиц предмета; каждый QUICK_MOVE перемещает один стак.
	 * Дефицит в единицах делится на размер стака слота, чтобы получить количество кликов.
	 *
	 * @param item      предмет для проверки
	 * @param slotStack стек из слота контейнера (нужен для получения maxCount)
	 */
	private int allowedClicksFor(Item item, ItemStack slotStack) {
		Integer targetItems = needed.get(item);

		if (targetItems == null) {
			return 0;
		}

		int inInventory = countItemsInPlayerInventory(item);
		int deficit = targetItems - inInventory;

		if (deficit <= 0) {
			return 0;
		}

		int stackSize = Math.max(1, slotStack.getMaxCount());

		return (int) Math.ceil((double) deficit / stackSize);
	}

	private void sendOpenPacket(BlockPos pos) {
		mc.interactionManager.interactBlock(
				mc.player,
				Hand.MAIN_HAND,
				new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false)
		);
	}

	private void finishCollect() {
		remainingBlocks = buildRemainingBlocks(context.getScreenHandler().slots, containerSize);
		debugInfo("[Collect] Завершено, syncId=" + context.getScreenHandler().syncId);

		PacketUtil.sendPacket(new CloseHandledScreenC2SPacket(context.getScreenHandler().syncId));
		context.close();
		opened = false;
		done = true;
	}

	private static List<CachedBlock> buildRemainingBlocks(DefaultedList<Slot> slots, int containerSize) {
		List<CachedBlock> result = new ArrayList<>();

		for (int i = 0; i < containerSize; i++) {
			ItemStack stack = slots.get(i).getStack();

			if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
				result.add(new CachedBlock(blockItem.getBlock(), stack.getCount()));
			}
		}

		return result;
	}

	private static int countItemsInPlayerInventory(Item item) {
		int count = 0;

		for (int i = 0; i < PLAYER_INVENTORY_SIZE; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);

			if (stack.isOf(item)) {
				count += stack.getCount();
			}
		}

		return count;
	}

	private static int countEmptySlotsInInventory() {
		int count = 0;

		for (int i = 0; i < PLAYER_INVENTORY_SIZE; i++) {
			if (mc.player.getInventory().getStack(i).isEmpty()) {
				count++;
			}
		}

		return count;
	}
}
