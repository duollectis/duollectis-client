package org.duollectis.client.modules;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.RenderEvent;
import org.duollectis.client.api.event.events.TickPostEvent;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.settings.ItemListSetting;
import org.duollectis.client.utils.color.Color;
import org.duollectis.client.utils.inventory.FindItemResult;
import org.duollectis.client.utils.inventory.InvUtility;
import org.duollectis.client.utils.inventory.InvUtils;
import org.duollectis.client.utils.network.PacketUtil;
import org.duollectis.client.utils.network.ScreenHandlerUtil;
import org.duollectis.client.utils.render.RenderUtils;
import org.duollectis.client.utils.rotation.RotationUtil;
import org.duollectis.client.utils.rotation.Rotations;
import org.duollectis.client.utils.world.MiscUtil;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.duollectis.client.utils.MC.mc;

/**
 * Модуль автоматического дюпа предметов через зомби-механику.
 * Управляет циклом: поиск нейм-тегов в сундуках, стакование предметов,
 * выброс мусора, подсчёт зомби и автоматическое открытие/закрытие контейнеров.
 * Отключается при переполнении инвентаря или нехватке шалкеров.
 */
@ModuleInfo(
		value = "zombie-duper",
		category = Category.MISC
)
public class ZombieDuper extends Module {

	private static final int OPEN_RETRY_DELAY = 10;
	private static final int NAMETAG_COOLDOWN_TICKS = 60;
	private static final double ZOMBIE_NEAR_DIST_SQ = 2.0;
	private static final double POSITION_SNAP_THRESHOLD = 0.05;
	private static final float ATTACK_COOLDOWN_THRESHOLD = 1.0F;
	private static final float ATTACK_COOLDOWN_PARTIAL = 0.5F;
	private static final double DROP_ROTATION_PITCH = -20.0;
	private static final double OFFSET_FAR = 0.7;
	private static final double OFFSET_NEAR = 0.3;
	private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

	{
		settings.add(BoolSetting
				.create("debug")
				.defaultValue(false)
				.onChanged(v -> debug = v)
				.done()
		);
	}

	private final Setting<Double> chestsRange = settings.add(DoubleSetting
			.create("chests-range")
			.defaultValue(5)
			.done()
	);

	private final Setting<Double> zombieRange = settings.add(DoubleSetting
			.create("zombie-range")
			.defaultValue(4)
			.done()
	);

	private final Setting<Double> countRange = settings.add(DoubleSetting
			.create("count-range")
			.defaultValue(2)
			.done()
	);

	private final Setting<Double> zombieTrashRange = settings.add(DoubleSetting
			.create("zombie-trash-range")
			.defaultValue(3)
			.done()
	);

	private final Setting<Double> speed = settings.add(DoubleSetting
			.create("speed")
			.defaultValue(0.1)
			.range(0.1, 1)
			.done()
	);

	private final Setting<Integer> delay = settings.add(IntSetting
			.create("delay")
			.defaultValue(3)
			.suffix("t")
			.done()
	);

	private final Setting<Integer> dropPerTick = settings.add(IntSetting
			.create("drop-per-tick")
			.min(1)
			.sliderRange(1, 36)
			.defaultValue(1)
			.done()
	);

	private final Setting<Integer> nametagAmount = settings.add(IntSetting
			.create("nametag-amount")
			.min(0)
			.range(1, 10)
			.defaultValue(5)
			.done()
	);

	private final Setting<Integer> nametagSlot = settings.add(IntSetting
			.create("nametag-slot")
			.range(1, 9)
			.defaultValue(5)
			.done()
	);

	private final Setting<Integer> refreshDelay = settings.add(IntSetting
			.create("refresh-delay")
			.min(1)
			.sliderRange(40, 20 * 60)
			.defaultValue(20 * 10)
			.done()
	);

	private final Setting<Integer> swordSlot = settings.add(IntSetting
			.create("sword-slot")
			.range(1, 9)
			.defaultValue(9)
			.done()
	);

	private final Setting<Integer> maxFullSlots = settings.add(IntSetting
			.create("max-full-slots")
			.min(1)
			.sliderRange(10, 36)
			.defaultValue(32)
			.done()
	);

	private final Setting<Integer> minShulkers = settings.add(IntSetting
			.create("min-shulkers")
			.min(1)
			.sliderRange(4, 30)
			.defaultValue(8)
			.done()
	);

	private final Setting<Boolean> disableOnSaturdayOver = settings.add(BoolSetting
			.create("disable-on-saturday-over")
			.defaultValue(true)
			.done()
	);

	private final Setting<List<Item>> trashItems = settings.add(ItemListSetting
			.create("trash-items")
			.defaultValue(List.of(
					Items.ROTTEN_FLESH,
					Items.CHICKEN,
					Items.COOKED_CHICKEN,
					Items.GOLDEN_BOOTS,
					Items.IRON_SHOVEL,
					Items.POTATO,
					Items.BAKED_POTATO,
					Items.IRON_INGOT,
					Items.IRON_SWORD,
					Items.GOLD_INGOT,
					Items.GOLDEN_HELMET,
					Items.CARROT,
					Items.FEATHER,
					Items.GOLDEN_CHESTPLATE,
					Items.GUNPOWDER
			))
			.done()
	);

	private final List<BlockPos> ignoredChests = new ArrayList<>();
	private final List<BlockPos> ignoredNametagChests = new ArrayList<>();
	private final Object2IntMap<Integer> cooldowns = new Object2IntOpenHashMap<>();

	private BlockPos zombiesPos;
	private BlockPos chestPos;

	private boolean openedChest;
	private boolean stacking;
	private boolean findNametags;

	private int nametagRefreshTimer;
	private int chestsRefreshTimer;
	private int openTimer;
	private int timer;

	private int shulkerCount;
	private int zombieCount;
	private int zombieCountAll;


	@Override
	public void onDeactivate() {
		zombiesPos = null;
		chestPos = null;
		ignoredChests.clear();
		ignoredNametagChests.clear();
		cooldowns.clear();

		timer = 0;
		openTimer = 0;
		nametagRefreshTimer = 0;
		chestsRefreshTimer = 0;

		openedChest = false;
		stacking = false;
		findNametags = false;

		shulkerCount = 0;
		zombieCount = 0;
		zombieCountAll = 0;
	}

	private void activate() {
		if (mc.world == null || mc.player == null) {
			return;
		}

		zombiesPos = mc.player.getBlockPos().up(2);
		byte sides = RotationUtil.getLookingSides();

		if (RotationUtil.isEast(sides)) {
			zombiesPos = zombiesPos.east();
		}

		if (RotationUtil.isWest(sides)) {
			zombiesPos = zombiesPos.west();
		}

		if (RotationUtil.isSouth(sides)) {
			zombiesPos = zombiesPos.south();
		}

		if (RotationUtil.isNorth(sides)) {
			zombiesPos = zombiesPos.north();
		}
	}

	@Subscribe
	private void onRender(RenderEvent event) {
		if (zombiesPos != null) {
			RenderUtils.drawLinesBox(
					event.getModelView(),
					zombiesPos,
					Color.GREEN.a(200).getPacked()
			);
		}

		if (chestPos != null) {
			RenderUtils.drawLinesBox(
					event.getModelView(),
					chestPos,
					Color.CYAN.a(200).getPacked()
			);
		}
	}

	private void dupe() {
		if (mc.player == null || mc.world == null) {
			return;
		}

		debugInfo("Rotating at zombies");
		mc.player.setPitch((float) Rotations.getPitch(zombiesPos));
		mc.player.setYaw((float) Rotations.getYaw(zombiesPos));

		debugInfo("Find nametags");
		FindItemResult nametagResult = InvUtils.findInHotbar(this::isNametag);

		if (!nametagResult.found()) {
			debugInfo("Hotbar not found");
			nametagResult = InvUtils.find(this::isNametag);

			if (nametagResult.found()) {
				debugInfo("Moving in hotbar");
				InvUtils.move().from(nametagResult.slot()).toHotbar(nametagSlot.get() - 1);
				return;
			}

			debugInfo("Enabling nametags finding");
			findNametags = true;
			return;
		}

		Vec3d playerPos = mc.player.getEntityPos();
		Vec3d offset = resolveMovementOffset(playerPos);

		double targetX = offset.x != 0 ? (int) playerPos.x : playerPos.x;
		double targetZ = offset.z != 0 ? (int) playerPos.z : playerPos.z;
		targetX += offset.x;
		targetZ += offset.z;

		double moveSpeed = speed.get();
		double diffX = targetX - playerPos.x;
		double diffZ = targetZ - playerPos.z;

		if (Math.abs(diffX) <= POSITION_SNAP_THRESHOLD && Math.abs(diffZ) <= POSITION_SNAP_THRESHOLD) {
			debugInfo("Standing");
			mc.player.setPosition(targetX, playerPos.y, targetZ);
		}
		else {
			debugInfo("Moving");
			mc.player.setPosition(
					playerPos.x + Math.max(-moveSpeed, Math.min(moveSpeed, diffX)),
					playerPos.y,
					playerPos.z + Math.max(-moveSpeed, Math.min(moveSpeed, diffZ))
			);
		}

		if (zombieCount > shulkerCount) {
			dropShulkersToZombies();
			return;
		}

		debugInfo("Renaming zombies");

		if (tryRenameZombie(nametagResult)) {
			return;
		}

		equipSword();

		if (mc.player.getAttackCooldownProgress(ATTACK_COOLDOWN_PARTIAL) < ATTACK_COOLDOWN_THRESHOLD) {
			debugInfo("Wait attack cd");
			return;
		}

		ZombieEntity firingZombie = getFiringZombie();

		if (firingZombie != null) {
			debugInfo("Attacking firing zombie");
			mc.interactionManager.attackEntity(mc.player, firingZombie);
			return;
		}

		if (hasZombieOutsideRange()) {
			return;
		}

		debugInfo("Attacking other zombies");

		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof ZombieEntity zombie && !isBadZombie(zombie)) {
				debugInfo("Attack " + entity.getId());
				mc.interactionManager.attackEntity(mc.player, entity);
				return;
			}
		}
	}

	private Vec3d resolveMovementOffset(Vec3d playerPos) {
		boolean shouldStayNear = zombieCountAll == 0
				|| (isHasTrashAroundZombies() && isHasEmptyZombie());

		Direction side = getShulkerSide(playerPos, zombiesPos.toCenterPos());
		Direction moveSide = shouldStayNear ? side : side.getOpposite();

		debugInfo(shouldStayNear ? "Stay near" : "Stay far");
		return getOffset(playerPos, RotationUtil.fromDirection(moveSide));
	}

	/**
	 * Бросает зомби шалкер того типа, которого меньше всего в инвентаре.
	 * Это выравнивает количество шалкеров каждого цвета и предотвращает
	 * ситуацию, когда один тип дюпится значительно больше остальных.
	 */
	private void dropShulkersToZombies() {
		if (mc.player == null) {
			return;
		}

		debugInfo("Dropping shulkers to zombies");

		if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
			mc.player.closeHandledScreen();
			return;
		}

		ItemStack targetType = findRarestShulkerType();

		if (targetType == null) {
			return;
		}

		for (int i = 0; i < mc.player.playerScreenHandler.slots.size(); i++) {
			ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();

			if (!ItemStack.areItemsAndComponentsEqual(stack, targetType)) {
				continue;
			}

			debugInfo("Drop slot " + i + " type=" + targetType.getItem());
			ScreenHandlerUtil.clickSlot(
					mc.player.playerScreenHandler,
					i,
					1,
					SlotActionType.THROW,
					mc.player
			);
			return;
		}
	}

	/**
	 * Находит наименее представленный тип шалкера в инвентаре, сравнивая по содержимому.
	 * Два шалкера одного цвета с разным содержимым считаются разными типами.
	 * Группировка выполняется через {@link ItemStack#areItemsAndComponentsEqual}.
	 */
	private ItemStack findRarestShulkerType() {
		List<Map.Entry<ItemStack, Integer>> groups = new ArrayList<>();

		for (int i = 0; i < mc.player.playerScreenHandler.slots.size(); i++) {
			ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();

			if (!isShulker(stack)) {
				continue;
			}

			boolean found = false;

			for (Map.Entry<ItemStack, Integer> entry : groups) {
				if (ItemStack.areItemsAndComponentsEqual(entry.getKey(), stack)) {
					entry.setValue(entry.getValue() + 1);
					found = true;
					break;
				}
			}

			if (!found) {
				groups.add(new java.util.AbstractMap.SimpleEntry<>(stack, 1));
			}
		}

		return groups.stream()
				.min(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	/**
	 * Проверяет, является ли данный шалкер последним своего типа (по содержимому) в инвентаре.
	 * Используется при стаковании: последний экземпляр каждого типа не складывается в сундук.
	 *
	 * @param prototype  эталонный стек шалкера для сравнения по содержимому
	 * @param invOffset  смещение до начала слотов инвентаря игрока в currentScreenHandler
	 */
	private boolean isLastOfType(ItemStack prototype, int invOffset) {
		int count = 0;

		for (int i = invOffset; i < invOffset + 36; i++) {
			if (ItemStack.areItemsAndComponentsEqual(mc.player.currentScreenHandler.getSlot(i).getStack(), prototype)) {
				count++;
			}
		}

		return count <= 1;
	}

	private boolean tryRenameZombie(FindItemResult nametagResult) {
		if (mc.world == null || mc.player == null) {
			return false;
		}

		for (Entity entity : mc.world.getEntities()) {
			if (!(entity instanceof ZombieEntity)) {
				continue;
			}

			ZombieEntity zombie = (ZombieEntity) entity;

			if (cooldowns.containsKey(entity.getId())) {
				debugInfo("Skip cd");
				continue;
			}

			if (entity.hasCustomName()) {
				debugInfo("Skip custom name");
				continue;
			}

			if (zombiesPos.getSquaredDistance(entity.getEntityPos()) > ZOMBIE_NEAR_DIST_SQ) {
				debugInfo("Skip distance");
				continue;
			}

			if (zombie.isDead() || !zombie.isAlive()) {
				debugInfo("Renaming dead");
				continue;
			}

			if (zombie.hasVehicle()) {
				debugInfo("Renaming vehicle");
				continue;
			}

			if (!isShulker(zombie.getOffHandStack()) && !isShulker(zombie.getMainHandStack())) {
				continue;
			}

			debugInfo("Renaming " + entity.getId());
			InvUtils.swap(nametagResult.slot(), true);
			PacketUtil.sendPacket(PlayerInteractEntityC2SPacket.interact(
					entity,
					mc.player.isSneaking(),
					Hand.MAIN_HAND
			));
			InvUtils.swapBack();
			cooldowns.put((Integer) entity.getId(), NAMETAG_COOLDOWN_TICKS);
			return true;
		}

		return false;
	}

	private void equipSword() {
		if (mc.player == null) {
			return;
		}

		debugInfo("Swap sword");
		FindItemResult swordResult = InvUtils.findInHotbar(
				stack -> stack.getItem().getComponents().contains(DataComponentTypes.WEAPON)
		);

		if (swordResult.found()) {
			InvUtils.swap(swordResult.slot(), false);
			return;
		}

		swordResult = InvUtils.find(
				stack -> stack.getItem().getComponents().contains(DataComponentTypes.WEAPON)
		);

		if (swordResult.found()) {
			InvUtils.move().from(swordResult.slot()).toHotbar(swordSlot.get() - 1);
			InvUtils.swap(swordSlot.get() - 1, false);
		}
	}

	private boolean hasZombieOutsideRange() {
		if (mc.world == null) {
			return false;
		}

		double rangeSquared = Math.pow(zombieRange.get(), 2);

		for (Entity entity : mc.world.getEntities()) {
			if (!(entity instanceof ZombieEntity)) {
				continue;
			}

			double distSq = zombiesPos.getSquaredDistance(entity.getEntityPos());

			if (distSq > rangeSquared) {
				continue;
			}

			if (distSq > ZOMBIE_NEAR_DIST_SQ) {
				return true;
			}
		}

		return false;
	}

	private boolean isNametag(ItemStack stack) {
		return stack.isOf(Items.NAME_TAG) && stack.get(DataComponentTypes.CUSTOM_NAME) != null;
	}

	private boolean isHasTrashAroundZombies() {
		if (mc.world == null) {
			return false;
		}

		for (Entity entity : mc.world.getEntities()) {
			if (!(entity instanceof ItemEntity itemEntity)) {
				continue;
			}

			ItemEntity itemEntity2 = (ItemEntity) entity;

			if (zombiesPos.toCenterPos().squaredDistanceTo(itemEntity2.getEntityPos()) >= ZOMBIE_NEAR_DIST_SQ) {
				continue;
			}

			if (!isShulker(itemEntity2.getStack())) {
				return true;
			}
		}

		return false;
	}

	private boolean isHasEmptyZombie() {
		if (mc.world == null) {
			return false;
		}

		double trashRangeSq = Math.pow(zombieTrashRange.get(), 2);

		for (Entity entity : mc.world.getEntities()) {
			if (!(entity instanceof ZombieEntity)) {
				continue;
			}

			ZombieEntity zombie = (ZombieEntity) entity;

			if (zombiesPos.toCenterPos().squaredDistanceTo(zombie.getEntityPos()) > trashRangeSq) {
				continue;
			}

			if (isZombieEmpty(zombie)) {
				return true;
			}
		}

		return false;
	}

	private boolean isZombieEmpty(LivingEntity entity) {
		return entity.getOffHandStack().isEmpty() && entity.getMainHandStack().isEmpty();
	}

	private boolean isBadZombie(LivingEntity entity) {
		if (entity.isDead() || !entity.isAlive()) {
			return true;
		}

		if (zombiesPos.getSquaredDistance(entity.getEntityPos()) > ZOMBIE_NEAR_DIST_SQ) {
			return true;
		}

		if (entity.getOffHandStack().isEmpty() && entity.getMainHandStack().isEmpty()) {
			return true;
		}

		return (isShulker(entity.getOffHandStack()) || isShulker(entity.getMainHandStack()))
				&& !entity.hasCustomName();
	}

	private ZombieEntity getFiringZombie() {
		if (mc.world == null) {
			return null;
		}

		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof ZombieEntity zombie && !isBadZombie(zombie) && zombie.isOnFire()) {
				return zombie;
			}
		}

		return null;
	}

	private boolean dropTrash() {
		if (mc.player == null) {
			return false;
		}

		debugInfo("Removing trash");

		int remaining = dropPerTick.get();
		boolean rotated = false;
		List<Slot> slots = mc.player.playerScreenHandler.slots;

		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i).getStack();

			if (!trashItems.get().contains(stack.getItem())) {
				continue;
			}

			if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
				mc.player.closeHandledScreen();
				return true;
			}

			debugInfo("Dropping slot " + i);

			if (!rotated) {
				PacketUtil.rotate(Rotations.getYaw(zombiesPos) - 180.0, DROP_ROTATION_PITCH);
				rotated = true;
			}

			ScreenHandlerUtil.clickSlot(
					mc.player.playerScreenHandler,
					i,
					1,
					SlotActionType.THROW,
					mc.player
			);

			remaining--;

			if (remaining <= 0) {
				PacketUtil.rotate(Rotations.getYaw(zombiesPos), Rotations.getPitch(zombiesPos));
				return true;
			}
		}

		if (rotated) {
			PacketUtil.rotate(Rotations.getYaw(zombiesPos), Rotations.getPitch(zombiesPos));
		}

		return false;
	}

	private void findNametags() {
		if (mc.player == null) {
			return;
		}

		FindItemResult result = InvUtils.find(this::isNametag);

		if (result.found()) {
			debugInfo("Stop finding nametags");
			findNametags = false;
			nametagRefreshTimer = 0;
			return;
		}

		if (nametagRefreshTimer > 0) {
			return;
		}

		MiscUtil.iterateStrict(
				chestsRange.get(), (x, y, z) -> {
					BlockPos pos = mc.player.getBlockPos().add(x, y, z);
					Block block = getBlock(pos);

					if ((block instanceof ChestBlock || block instanceof ShulkerBoxBlock)
							&& !ignoredNametagChests.contains(pos)) {
						debugInfo("Opening container");
						chestPos = pos;
						ensureContainerOpen(pos);
						return true;
					}

					return false;
				}
		);

		if (openedChest) {
			if (!ensureContainerOpen(chestPos)) {
				debugInfo("Nametags container not opened, waiting");
				return;
			}
		}
		else {
			nametagRefreshTimer = refreshDelay.get();
			ignoredNametagChests.clear();
			warning("No nametags found, waiting...");
			return;
		}

		int remaining = nametagAmount.get();

		debugInfo("Finding slots");

		for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
			if (!isNametag(mc.player.currentScreenHandler.getSlot(i).getStack())) {
				continue;
			}

			debugInfo("Found slot " + i);
			ScreenHandlerUtil.clickSlot(
					mc.player.currentScreenHandler,
					i,
					0,
					SlotActionType.QUICK_MOVE,
					mc.player
			);

			remaining--;

			if (remaining <= 0) {
				debugInfo("All picked and closing chest");
				closeChest();
				return;
			}
		}

		debugInfo("Ignoring chest");
		ignoredNametagChests.add(chestPos);
		closeChest();
	}

	private void stack() {
		if (mc.player == null) {
			return;
		}

		if (!isContainerScreenOpen() && dropTrash()) {
			return;
		}

		if (chestsRefreshTimer > 0) {
			return;
		}

		if (openedChest && !isContainerScreenOpen()) {
			if (openTimer <= 0) {
				if (chestPos == null) {
					openedChest = false;
				}
				else {
					ensureContainerOpen(chestPos);
				}
			}

			return;
		}

		if (InvUtility.countShulkers() > minShulkers.get()) {
			int offset = mc.player.currentScreenHandler.slots.size() - 36;

			for (int i = offset; i < offset + 36; i++) {
				ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();

				if (!isShulker(stack)) {
					continue;
				}

				// Не складываем последний экземпляр данного типа шалкера — он нужен для дюпа
				if (isLastOfType(stack, offset)) {
					continue;
				}

				MiscUtil.iterateStrict(
						chestsRange.get(), (x, y, z) -> {
							BlockPos pos = mc.player.getBlockPos().add(x, y, z);

							if (getBlock(pos) == Blocks.CHEST
									&& !ignoredChests.contains(pos)
									&& !openedChest) {
								chestPos = pos;
								ensureContainerOpen(pos);
								return true;
							}

							return false;
						}
				);

				if (openedChest) {
					if (!ensureContainerOpen(chestPos)) {
						return;
					}
				} else {
					chestsRefreshTimer = refreshDelay.get();
					ignoredChests.clear();
					warning("No empty chests found, waiting...");
					return;
				}

				if (isChestFull()) {
					ignoredChests.add(chestPos);
					closeChest();
					return;
				}

				ScreenHandlerUtil.clickSlot(
						mc.player.currentScreenHandler,
						i,
						0,
						SlotActionType.QUICK_MOVE,
						mc.player
				);

				return;
			}
		}

		closeChest();
		stacking = false;
	}

	private void closeChest() {
		if (mc.player == null) {
			return;
		}

		mc.player.closeHandledScreen();
		openedChest = false;
		chestPos = null;
	}

	private void countZombies() {
		if (mc.world == null) {
			return;
		}

		shulkerCount = 0;
		zombieCount = 0;
		zombieCountAll = 0;

		double countRangeSq = Math.pow(countRange.get(), 2);

		for (Entity entity : mc.world.getEntities()) {
			if (entity == null || zombiesPos.getSquaredDistance(entity.getEntityPos()) > countRangeSq) {
				continue;
			}

			if (entity instanceof ItemEntity itemEntity && isShulker(itemEntity.getStack())) {
				shulkerCount++;
				continue;
			}

			if (!(entity instanceof ZombieEntity)) {
				continue;
			}

			ZombieEntity zombie = (ZombieEntity) entity;

			if (zombie.isDead() || !zombie.isAlive()) {
				continue;
			}

			zombieCountAll++;

			if (zombie.getOffHandStack().isEmpty() && zombie.getMainHandStack().isEmpty()) {
				zombieCount++;
			}
		}
	}

	private boolean ensureContainerOpen(BlockPos pos) {
		if (mc.player == null || mc.interactionManager == null) {
			return false;
		}

		if (pos == null) {
			openedChest = false;
			return false;
		}

		if (isContainerScreenOpen()) {
			return true;
		}

		if (!openedChest || openTimer <= 0) {
			openedChest = true;
			openTimer = OPEN_RETRY_DELAY;

			BlockHitResult hitResult = new BlockHitResult(
					pos.toCenterPos().add(0, 0.5, 0),
					Direction.UP,
					pos,
					false
			);

			mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
			mc.player.swingHand(Hand.MAIN_HAND);
		}

		return false;
	}

	private boolean isContainerScreenOpen() {
		if (mc.player == null || mc.currentScreen == null || mc.player.currentScreenHandler == null) {
			return false;
		}

		ScreenHandlerType<?> type;

		try {
			type = mc.player.currentScreenHandler.getType();
		}
		catch (UnsupportedOperationException e) {
			return false;
		}

		return ScreenHandlerType.GENERIC_9X3.equals(type)
				|| ScreenHandlerType.GENERIC_9X6.equals(type)
				|| ScreenHandlerType.SHULKER_BOX.equals(type);
	}

	private boolean isChestFull() {
		for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
			if (mc.player.currentScreenHandler.getSlot(i).getStack().isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Subscribe
	private void onTickPre(TickPreEvent event) {
		if (mc.player == null || mc.world == null) {
			return;
		}

		if (disableOnSaturdayOver.get() && !isSaturdayInMoscow()) {
			infoDisable("Saturday is over! Disabling!");

			AutoReconnect autoReconnect = ModuleManager.getInstance().get(AutoReconnect.class);
			autoReconnect.disable();

			if (mc.getNetworkHandler() != null) {
				mc.getNetworkHandler().getConnection().disconnect(Text.literal("Saturday is done!"));
			}

			mc.setScreen(new DisconnectedScreen(
					mc.currentScreen,
					Text.literal("Saturday is done!"),
					Text.literal("Dupe ended.")
			));

			return;
		}

		if (zombiesPos == null) {
			activate();
			return;
		}

		if (timer > 0) {
			return;
		}

		timer = delay.get();

		countZombies();

		if (findNametags) {
			debugInfo("Finding nametags tick");
			findNametags();
			return;
		}

		if (zombieCountAll <= 0 && (stacking || InvUtility.countFullSlots() >= maxFullSlots.get())) {
			debugInfo("Stack tick");
			stacking = true;
			stack();
		}
		else {
			debugInfo("Dupe tick");
			openedChest = false;
			chestPos = null;
			stacking = false;

			dupe();
		}
	}

	@Subscribe
	private void onTickPost(TickPostEvent event) {
		nametagRefreshTimer--;
		chestsRefreshTimer--;
		openTimer--;
		timer--;

		Iterator<Integer> it = cooldowns.keySet().iterator();

		while (it.hasNext()) {
			Integer id = it.next();
			int cooldown = cooldowns.getInt(id) - 1;

			if (cooldown <= 0) {
				it.remove();
			}
			else {
				cooldowns.put(id, cooldown);
			}
		}
	}

	private static boolean isShulker(ItemStack stack) {
		if (stack == null) {
			return false;
		}

		return stack.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof ShulkerBoxBlock;
	}

	public static Direction getShulkerSide(Vec3d playerPos, Vec3d shulkerPos) {
		double dx = shulkerPos.getX() - playerPos.getX();
		double dz = shulkerPos.getZ() - playerPos.getZ();

		return Math.abs(dx) > Math.abs(dz)
		       ? (dx > 0 ? Direction.EAST : Direction.WEST)
		       : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
	}

	public static Block getBlock(BlockPos blockPos) {
		return mc.world.getBlockState(blockPos).getBlock();
	}

	public static Vec3d getOffset(Vec3d pos, byte flags) {
		double x = 0;
		double z = 0;

		if (pos.x >= 0) {
			if (RotationUtil.isEast(flags)) {
				x = OFFSET_FAR;
			}
			else if (RotationUtil.isWest(flags)) {
				x = OFFSET_NEAR;
			}
		}
		else {
			if (RotationUtil.isEast(flags)) {
				x = -OFFSET_NEAR;
			}
			else if (RotationUtil.isWest(flags)) {
				x = -OFFSET_FAR;
			}
		}

		if (pos.z >= 0) {
			if (RotationUtil.isSouth(flags)) {
				z = OFFSET_FAR;
			}
			else if (RotationUtil.isNorth(flags)) {
				z = OFFSET_NEAR;
			}
		}
		else {
			if (RotationUtil.isSouth(flags)) {
				z = -OFFSET_NEAR;
			}
			else if (RotationUtil.isNorth(flags)) {
				z = -OFFSET_FAR;
			}
		}

		return new Vec3d(x, 0, z);
	}

	public static boolean isSaturdayInMoscow() {
		return ZonedDateTime.now(MOSCOW_ZONE).getDayOfWeek() == DayOfWeek.SATURDAY;
	}
}
