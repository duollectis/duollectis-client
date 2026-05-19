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
import org.duollectis.client.settings.*;
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
import java.util.List;

@ModuleInfo(
		value = "zombie-duper",
		category = Category.MISC
)
public class ZombieDuper extends Module {

	private static final int OPEN_RETRY_DELAY = 10;

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

	private boolean openedChest;
	private boolean stacking;
	private boolean findNametags;
	private int nametagRefreshTimer;
	private int chestsRefreshTimer;

	private int openTimer;

	private BlockPos chestPos;

	private int timer;

	private int shulkerCount;
	private int zombieCount;
	private int zombieCountAll;

	@Override
	public void onDeactivate() {
		zombiesPos = null;
		ignoredChests.clear();
		ignoredNametagChests.clear();
		cooldowns.clear();
		timer = 0;
		openTimer = 0;
		findNametags = false;
		nametagRefreshTimer = 0;
		chestsRefreshTimer = 0;

		openedChest = false;
		stacking = false;
		chestPos = null;

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
					event.getMatrices(),
					zombiesPos,
					Color.GREEN.a(200).getPacked()
			);
		}

		if (chestPos != null) {
			RenderUtils.drawLinesBox(
					event.getMatrices(),
					chestPos,
					Color.CYAN.a(200).getPacked()
			);
		}
	}

	private void dupe() {
		assert mc.player != null;
		assert mc.world != null;

		debugInfo("Rotating at zombies");
		mc.player.setPitch((float) Rotations.getPitch(zombiesPos));
		mc.player.setYaw((float) Rotations.getYaw(zombiesPos));


		debugInfo("Find nametags");
		FindItemResult result = InvUtils.findInHotbar(this::isNametag);

		if (!result.found()) {
			debugInfo("Hotbar not found");
			result = InvUtils.find(this::isNametag);

			if (result.found()) {
				debugInfo("Moving in hotbar");

				InvUtils.move().from(result.slot()).toHotbar(nametagSlot.get() - 1);
				return;
			}
			else {
				debugInfo("Enabling nametags finding");
				findNametags = true;
				return;
			}
		}

		Vec3d ppos = mc.player.getEntityPos();
		double x = ppos.x;
		double z = ppos.z;

		Vec3d offset;

		if (zombieCountAll == 0 || (isHasTrashAroundZombies() && isHasEmptyZombie())) {
			debugInfo("Stay near");

			offset = getOffset(
					mc.player.getEntityPos(),
					RotationUtil.fromDirection(getShulkerSide(mc.player.getEntityPos(), zombiesPos.toCenterPos()))
			);
		}
		else {
			debugInfo("Stay far");

			offset = getOffset(
					mc.player.getEntityPos(),
					RotationUtil.fromDirection(getShulkerSide(
							mc.player.getEntityPos(),
							zombiesPos.toCenterPos()
					).getOpposite())
			);
		}

		if (offset.x != 0)
			x = (int) x;
		if (offset.z != 0)
			z = (int) z;

		double targetX = x + offset.x;
		double targetZ = z + offset.z;
		double speed = this.speed.get();
		double diffX = targetX - ppos.x;
		double diffZ = targetZ - ppos.z;
		if (!(Math.abs(diffX) > 0.05) && !(Math.abs(diffZ) > 0.05)) {
			debugInfo("Standing");
			mc.player.setPosition(targetX, ppos.y, targetZ);
		}
		else {
			debugInfo("Moving");

			mc.player.setPosition(
					ppos.x + Math.max(-speed, Math.min(speed, diffX)), ppos.y,
					ppos.z + Math.max(-speed, Math.min(speed, diffZ))
			);
		}

		if (zombieCount > shulkerCount) {
			debugInfo("Dropping shulkers to zombies");

			if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
				mc.player.closeHandledScreen();
				return;
			}

			for (int i = 0; i < mc.player.playerScreenHandler.slots.size(); i++) {
				ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();

				if (isShulker(stack)) {
					debugInfo("Drop slot " + i);

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
		}

		debugInfo("Renaming zombies");
		for (Entity entity : mc.world.getEntities()) {

			if (!(entity instanceof ZombieEntity zombie)) {
				continue;
			}

			if (cooldowns.containsKey(entity.getId())) {
				debugInfo("Skip cd");
				continue;
			}

			if (entity.hasCustomName()) {
				debugInfo("Skip custom name");
				continue;
			}

			if (zombiesPos.getSquaredDistance(entity.getEntityPos()) > 2) {
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

			if (isShulker(zombie.getOffHandStack()) || isShulker(zombie.getMainHandStack())) {
				debugInfo("Renaming " + entity.getId());


				InvUtils.swap(result.slot(), true);

				PacketUtil.sendPacket(PlayerInteractEntityC2SPacket.interact(
						entity,
						mc.player.isSneaking(),
						Hand.MAIN_HAND
				));

				//					mc.interactionManager.interactEntity(
				//							mc.player,
				//							entity,
				//							Hand.MAIN_HAND
				//					);

				InvUtils.swapBack();

				cooldowns.put(Integer.valueOf(entity.getId()), 60);

				return;
			}
		}

		debugInfo("Swap sword");

		FindItemResult swordResult = InvUtils.findInHotbar(stack -> stack
				.getItem()
				.getComponents()
				.contains(DataComponentTypes.WEAPON)
		);

		if (swordResult.found()) {
			InvUtils.swap(swordResult.slot(), false);
		}
		else {
			swordResult = InvUtils.find(stack -> stack
					.getItem()
					.getComponents()
					.contains(DataComponentTypes.WEAPON)
			);

			if (swordResult.found()) {
				InvUtils.move().from(swordResult.slot()).toHotbar(swordSlot.get() - 1);
				InvUtils.swap(swordSlot.get() - 1, false);
			}
		}

		if (mc.player.getAttackCooldownProgress(0.5F) < 1.0F) {
			debugInfo("Wait attack cd");
			return;
		}

		ZombieEntity firingZombie = getFiringZombie();

		if (firingZombie == null) {
			for (Entity entity : mc.world.getEntities()) {
				if (entity instanceof ZombieEntity zombie) {
					if (zombiesPos.getSquaredDistance(zombie.getEntityPos()) > Math.pow(zombieRange.get(), 2)) {
						continue;
					}

					if (zombiesPos.getSquaredDistance(zombie.getEntityPos()) > 2) {
						return;
					}
				}
			}
		}
		else {
			debugInfo("Attacking firing zombie");

			assert mc.interactionManager != null;
			mc.interactionManager.attackEntity(mc.player, firingZombie);
			return;
		}

		assert mc.world != null;

		debugInfo("Attacking other zombies");
		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof ZombieEntity zombie) {
				if (isBadZombie(zombie)) {
					continue;
				}

				debugInfo("Attack " + entity.getId());

				assert mc.interactionManager != null;
				mc.interactionManager.attackEntity(mc.player, entity);
				return;
			}
		}
	}

	private boolean isNametag(ItemStack stack) {
		return stack.isOf(Items.NAME_TAG) && stack.get(DataComponentTypes.CUSTOM_NAME) != null;
	}

	private boolean isHasTrashAroundZombies() {
		assert mc.world != null;

		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof ItemEntity item) {
				if (zombiesPos.toCenterPos().squaredDistanceTo(item.getEntityPos()) < 2) {

					if (!isShulker(item.getStack())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean isHasEmptyZombie() {
		assert mc.world != null;

		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof ZombieEntity zombie) {
				if (zombiesPos.toCenterPos().squaredDistanceTo(zombie.getEntityPos()) <= Math.pow(
						zombieTrashRange.get(),
						2
				)) {

					if (isZombieEmpty(zombie)) {
						return true;
					}
				}
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

		if (zombiesPos.getSquaredDistance(entity.getEntityPos()) > 2) {
			return true;
		}

		if (entity.getOffHandStack().isEmpty() && entity.getMainHandStack().isEmpty()) {
			return true;
		}

		return (isShulker(entity.getOffHandStack()) || isShulker(entity.getMainHandStack()))
				&& !entity.hasCustomName();
	}

	private ZombieEntity getFiringZombie() {
		assert mc.world != null;
		for (Entity entity : mc.world.getEntities()) {
			if (entity instanceof ZombieEntity zombie) {
				if (isBadZombie(zombie)) {
					continue;
				}

				if (zombie.isOnFire()) {
					return zombie;
				}
			}
		}

		return null;
	}

	private boolean dropTrash() {
		assert mc.player != null;

		debugInfo("Removing trash");

		int dpt = dropPerTick.get();
		boolean rotated = false;

		List<Slot> slots = mc.player.playerScreenHandler.slots;

		for (int i = 0; i < slots.size(); i++) {
			ItemStack stack = slots.get(i).getStack();

			if (trashItems.get().contains(stack.getItem())) {

				if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
					mc.player.closeHandledScreen();
					return true;
				}

				debugInfo("Dropping slot " + i);

				if (!rotated) {
					PacketUtil.rotate(Rotations.getYaw(zombiesPos) - 180.0, -20.0);
					rotated = true;
				}

				ScreenHandlerUtil.clickSlot(
						mc.player.playerScreenHandler,
						i,
						1,
						SlotActionType.THROW,
						mc.player
				);

				dpt--;

				if (dpt <= 0) {

					PacketUtil.rotate(
							Rotations.getYaw(zombiesPos),
							Rotations.getPitch(zombiesPos)
					);

					return true;
				}
			}
		}

		if (rotated) {
			PacketUtil.rotate(
					Rotations.getYaw(zombiesPos),
					Rotations.getPitch(zombiesPos)
			);
		}

		return false;
	}

	private void findNametags() {

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

					if (block instanceof ChestBlock || block instanceof ShulkerBoxBlock) {

						if (!ignoredNametagChests.contains(pos)) {

							debugInfo("Opening container");

							chestPos = pos;
							ensureContainerOpen(pos);

							return true;
						}

					}

					return false;
				}
		);

		if (openedChest) {
			if (!ensureContainerOpen(chestPos)) {
				debugInfo("Nametags container not opened, waitinig");
				return;
			}
		}
		else {
			nametagRefreshTimer = refreshDelay.get();
			ignoredNametagChests.clear();
			warning("No nametags found, waiting...");
			return;
		}

		int nametags = nametagAmount.get();

		debugInfo("Finding slots");
		for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
			if (isNametag(mc.player.currentScreenHandler.getSlot(i).getStack())) {

				debugInfo("Found slot " + i);

				ScreenHandlerUtil.clickSlot(
						mc.player.currentScreenHandler,
						i,
						0,
						SlotActionType.QUICK_MOVE,
						mc.player
				);

				nametags--;

				if (nametags <= 0) {
					debugInfo("All picked and closing chest");
					closeChest();
					return;
				}
			}
		}

		debugInfo("Ignoring chest");

		ignoredNametagChests.add(chestPos);
		closeChest();
	}

	private void stack() {
		assert mc.player != null;

		if (!isContainerScreenOpen() && dropTrash()) {
			return;
		}

		if (chestsRefreshTimer > 0) {
			return;
		}

		if (openedChest && !isContainerScreenOpen()) {
			if (openTimer <= 0) { // Если время ожидания вышло
				if (chestPos == null) {
					openedChest = false;
				}
				else {
					ensureContainerOpen(chestPos); // Повторная попытка
				}
			}
			return;
		}

		if (InvUtility.countShulkers() > minShulkers.get()) {
			int offset = mc.player.currentScreenHandler.slots.size() - 36;
			for (int i = offset; i < offset + 36; i++) {
				ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();

				if (isShulker(stack)) {
					MiscUtil.iterateStrict(
							chestsRange.get(), (x, y, z) -> {
								BlockPos pos = mc.player.getBlockPos().add(x, y, z);

								if (getBlock(pos) == Blocks.CHEST && !ignoredChests.contains(pos)) {
									if (!openedChest) {
										chestPos = pos;
										ensureContainerOpen(pos);
										return true;
									}
								}

								return false;
							}
					);

					if (openedChest) {
						if (!ensureContainerOpen(chestPos)) {
							return;
						}
					}
					else {
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
		}

		closeChest();
		stacking = false;
	}

	private void closeChest() {
		assert mc.player != null;
		mc.player.closeHandledScreen();
		openedChest = false;
		chestPos = null;
	}

	private void countZombies() {
		assert mc.world != null;

		shulkerCount = 0;
		zombieCount = 0;
		zombieCountAll = 0;

		for (Entity entity : mc.world.getEntities()) {
			if (entity == null || zombiesPos.getSquaredDistance(entity.getEntityPos()) > Math.pow(
					countRange.get(),
					2
			)) {
				continue;
			}

			if (entity instanceof ItemEntity item && isShulker(item.getStack())) {
				shulkerCount++;
				continue;
			}

			if (entity instanceof ZombieEntity zombie) {
				if (zombie.isDead() || !zombie.isAlive()) {
					continue;
				}

				zombieCountAll++;
				if (zombie.getOffHandStack().isEmpty() && zombie.getMainHandStack().isEmpty()) {
					zombieCount++;
				}
			}
		}

	}

	private boolean ensureContainerOpen(BlockPos pos) {
		assert mc.player != null;
		assert mc.interactionManager != null;

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
		assert mc.player != null;

		if (mc.currentScreen == null || mc.player.currentScreenHandler == null) {
			return false;
		}

		ScreenHandlerType<?> type;

		try {
			type = mc.player.currentScreenHandler.getType();
		}
		catch (UnsupportedOperationException e) {
			return false;
		}

		return ScreenHandlerType.GENERIC_9X3.equals(type) ||
				ScreenHandlerType.GENERIC_9X6.equals(type) ||
				ScreenHandlerType.SHULKER_BOX.equals(type);
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

		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
	}

	public static Direction getShulkerSide(Vec3d playerPos, Vec3d shulkerPos) {
		double dx = shulkerPos.getX() - playerPos.getX();
		double dz = shulkerPos.getZ() - playerPos.getZ();

		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		}

		return dz > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	public static Block getBlock(BlockPos blockPos) {
		assert mc.world != null;
		return mc.world.getBlockState(blockPos).getBlock();
	}

	public static Vec3d getOffset(Vec3d pos, byte flags) {
		double x = 0, z = 0;

		if (pos.x >= 0) {
			if (RotationUtil.isEast(flags))
				x = 0.7;
			else if (RotationUtil.isWest(flags))
				x = 0.3;
		}
		else if (RotationUtil.isEast(flags))
			x = -0.3;
		else if (RotationUtil.isWest(flags))
			x = -0.7;

		if (pos.z >= 0) {
			if (RotationUtil.isSouth(flags))
				z = 0.7;
			else if (RotationUtil.isNorth(flags))
				z = 0.3;
		}
		else if (RotationUtil.isSouth(flags))
			z = -0.3;
		else if (RotationUtil.isNorth(flags))
			z = -0.7;

		return new Vec3d(x, 0, z);
	}

	private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

	public static boolean isSaturdayInMoscow() {
		// 1. Получаем текущее время конкретно в часовом поясе Москвы (UTC+3)
		ZonedDateTime moscowTime = ZonedDateTime.now(MOSCOW_ZONE);

		// 2. Проверяем, равен ли текущий день недели субботе
		return moscowTime.getDayOfWeek() == DayOfWeek.SATURDAY;
	}
}
