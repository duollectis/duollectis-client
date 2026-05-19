package org.duollectis.client.modules;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.duollectis.client.api.Friends;
import org.duollectis.client.api.event.Subscribe;
import org.duollectis.client.api.event.events.TickPreEvent;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.*;
import org.duollectis.client.utils.color.Color;
import org.duollectis.client.utils.inventory.InvUtility;
import org.duollectis.client.utils.text.TextUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
		value = "auto-log"
)
public class AutoLog extends Module {

	private static final String GROUP_GENERAL = "General";
	private static final String GROUP_LOG_MESSAGES = "Log Messages";

	public enum LogMode {
		Kick,
		Quit
	}

	public enum TriggerMode {
		Health,
		Totems,
		None
	}

	private final Setting<LogMode> logMode = settings.add(EnumSetting
			.<LogMode>create("log-mode")
			.group(GROUP_GENERAL)
			.defaultValue(LogMode.Quit)
			.done()
	);

	private final Setting<TriggerMode> triggerMode = settings.add(EnumSetting
			.<TriggerMode>create("trigger-mode")
			.group(GROUP_GENERAL)
			.defaultValue(TriggerMode.Health)
			.done()
	);

	private final Setting<Integer> health = settings.add(IntSetting
			.create("health")
			.group(GROUP_GENERAL)
			.defaultValue(6)
			.range(0, 20)
			.sliderRange(0, 20)
			.visible(() -> triggerMode.get() == TriggerMode.Health)
			.done()
	);

	private final Setting<Boolean> noTotemsOnly = settings.add(BoolSetting
			.create("only-no-totems")
			.group(GROUP_GENERAL)
			.defaultValue(true)
			.visible(() -> triggerMode.get() == TriggerMode.Health)
			.done()
	);

	private final Setting<Integer> totemsCount = settings.add(IntSetting
			.create("totems-count")
			.group(GROUP_GENERAL)
			.defaultValue(2)
			.min(0)
			.sliderRange(0, 37)
			.visible(() -> triggerMode.get() == TriggerMode.Totems)
			.done()
	);

	private final Setting<Boolean> toggleOff = settings.add(BoolSetting
			.create("toggle-off")
			.group(GROUP_GENERAL)
			.defaultValue(true)
			.done()
	);

	private final Setting<Boolean> ignoreIfNaked = settings.add(BoolSetting
			.create("ignore-if-naked")
			.group(GROUP_GENERAL)
			.defaultValue(true)
			.done()
	);

	private final Setting<List<Item>> ignoredItems = settings.add(ItemListSetting
			.create("ignores-items")
			.group(GROUP_GENERAL)
			.defaultValue(List.of(Items.WOODEN_SWORD))
			.visible(ignoreIfNaked::get)
			.done()
	);

	private final Setting<Boolean> logMessagesActive = settings.add(BoolSetting
			.create("log-messages")
			.group(GROUP_LOG_MESSAGES)
			.defaultValue(true)
			.done()
	);

	private final Setting<Boolean> showNearbyPlayers = settings.add(BoolSetting
			.create("show-nearby-players")
			.group(GROUP_LOG_MESSAGES)
			.defaultValue(true)
			.done()
	);

	private final Setting<Integer> nearbyPlayerRange = settings.add(IntSetting
			.create("nearby-players-range")
			.group(GROUP_LOG_MESSAGES)
			.defaultValue(30)
			.min(0)
			.sliderRange(5, 40)
			.visible(showNearbyPlayers::get)
			.done()
	);

	private final Setting<Boolean> ignoreFriends = settings.add(BoolSetting
			.create("ignore-friends")
			.group(GROUP_LOG_MESSAGES)
			.defaultValue(true)
			.done()
	);

	private final List<String> nearbyPlayers = new ArrayList<>();

	@Subscribe
	private void onTickPre(TickPreEvent event) {
		if (mc.player == null || mc.world == null || mc.player.isCreative() || mc.player.isSpectator()) {
			return;
		}

		if (ignoreIfNaked.get() && InvUtility.isInventoryEmpty(ignoredItems.get())) {
			return;
		}

		int playerHealth = (int) mc.player.getHealth();

		if (showNearbyPlayers.get()) {
			collectNearbyPlayers();
		}

		switch (triggerMode.get()) {
			case Health -> {
				if (noTotemsOnly.get() == false || InvUtility.itemContains(Items.TOTEM_OF_UNDYING, true) == false) {
					if (playerHealth <= health.get()) {
						MutableText text = TextUtil.createText(
								new TextUtil.TextSettings("Health was lower than ", new Color(0xFF3636), true)
						);
						text.append(TextUtil.createText(
								new TextUtil.TextSettings(
										health.get() + " ",
										new Color(0xA381FF),
										true,
										false,
										true,
										false,
										false
								)
						));
						text.append(TextUtil.createText(
								new TextUtil.TextSettings("Left: ", new Color(0xB93B28), true)
						));
						text.append(TextUtil.createText(
								new TextUtil.TextSettings(
										playerHealth + " ",
										new Color(0xDF8BFF),
										true,
										false,
										true,
										false,
										false
								)
						));
						text.append(concNearbyPlayers());

						_log(text);
					}
				}
			}
			case Totems -> {
				int totems = InvUtility.countItem(Items.TOTEM_OF_UNDYING);
				if (totems <= totemsCount.get()) {
					_log(TextUtil
							.createText(new TextUtil.TextSettings("Totems left:", new Color(0xFFCA59), true))
							.append(TextUtil
									.createText(new TextUtil.TextSettings(" " + totems, true))
									.append(concNearbyPlayers())));
				}
			}
		}
	}

	private void collectNearbyPlayers() {
		assert mc.world != null;

		nearbyPlayers.clear();

		for (PlayerEntity player : mc.world.getPlayers()) {
			if (player == mc.player) {
				continue;
			}

			int nearbyRange = nearbyPlayerRange.get();

			if (player.squaredDistanceTo(mc.player) > nearbyRange * nearbyRange) {
				continue;
			}

			if (ignoreFriends.get() && Friends.getInstance().isFriend(player)) {
				continue;
			}

			nearbyPlayers.add(player.getName().getString());
		}
	}

	private Text concNearbyPlayers() {
		if (showNearbyPlayers.get() == false || nearbyPlayers.isEmpty()) {
			return Text.empty();
		}

		MutableText text = TextUtil.createText(
				new TextUtil.TextSettings("\nNearby players:\n", new Color(0x5A2AFF), true)
		);
		text.append(TextUtil.createText(new TextUtil.TextSettings(nearbyPlayers + ".")));

		return text;
	}

	private void _log(Text message) {
		if (ignoreIfNaked.get() && InvUtility.isInventoryEmpty(ignoredItems.get())) {
			return;
		}

		log(message);

		if (toggleOff.get()) {
			disable();
		}
	}

	public void log(Text message) {
		assert mc.player != null && mc.world != null;

		AutoReconnect autoReconnect = ModuleManager.getInstance().get(AutoReconnect.class);
		autoReconnect.disable();

		Screen screen = createScreen(message);

		switch (logMode.get()) {
			case Kick -> {
				warning("AutoLog triggered in Kick mode!");
			}
			case Quit -> {
				mc.getNetworkHandler().getConnection().disconnect(Text.literal("Connection lost."));
				mc.setScreen(screen);
			}
		}
	}

	private static Screen createScreen(Text message) {
		MutableText text = TextUtil.createGradientText(
				new TextUtil.TextSettings("Triggered Auto Log.", true),
				new Color(0xFFC698),
				new Color(0xDE7E00)
		);

		return new DisconnectedScreen(new TitleScreen(), text, message);
	}
}
