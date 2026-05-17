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
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleInfo;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.settings.*;
import org.duollectis.client.utils.Color;
import org.duollectis.client.utils.InvUtility;
import org.duollectis.client.utils.TextUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
		name = "auto-log",
		description = "Disconnects from the server if u can die."
)
public class AutoLog extends Module {

	public enum LogMode {
		Kick,
		Quit
	}

	public enum TriggerMode {
		Health,
		Totems,
		None
	}

	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final SettingGroup sgLogMessages = settings.createGroup("Log Messages");

	private final Setting<LogMode> logMode = sgGeneral.add(new EnumSetting.Builder<LogMode>()
			.name("log-mode")
			.defaultValue(LogMode.Quit)
			.build()
	);


	//    private final Setting<Boolean> lagCheck = sgGeneral.add(new BoolSetting.Builder()
	//        .name("lag-mode")
	//        .description("Disconnects when no answer from the server.")
	//        .defaultValue(true)
	//        .build()
	//    );
	//
	//    private final Setting<Double> tickTime = sgGeneral.add(new DoubleSetting.Builder()
	//        .name("last-tick-time")
	//        .description("Time after the last tick.")
	//        .defaultValue(6.0)
	//        .build()
	//    );

	private final Setting<TriggerMode> triggerMode = sgGeneral.add(new EnumSetting.Builder<TriggerMode>()
			.name("trigger-mode")
			.defaultValue(TriggerMode.Health)
			.build()
	);

	private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
			.name("health")
			.description("Automatically disconnects when health is lower or equal to this value.")
			.defaultValue(6)
			.range(0, 20)
			.sliderRange(0, 20)
			.visible(() -> triggerMode.get() == TriggerMode.Health)
			.build()
	);


	private final Setting<Boolean> noTotemsOnly = sgGeneral.add(new BoolSetting.Builder()
			.name("only-no-totems")
			.description("Health mode works if no totems only.")
			.defaultValue(true)
			.visible(() -> triggerMode.get() == TriggerMode.Health)
			.build()
	);


	private final Setting<Integer> totemsCount = sgGeneral.add(new IntSetting.Builder()
			.name("totems-count")
			.description("Disconnects when totems count is lower or equal this value.")
			.defaultValue(2)
			.min(0)
			.sliderRange(0, 37)
			.visible(() -> triggerMode.get() == TriggerMode.Totems)
			.build()
	);

	private final Setting<Boolean> toggleOff = sgGeneral.add(new BoolSetting.Builder()
			.name("toggle-off")
			.description("Disables Auto Log after usage.")
			.defaultValue(true)
			.build()
	);

	private final Setting<Boolean> ignoreIfNaked = sgGeneral.add(new BoolSetting.Builder()
			.name("ignore-if-naked")
			.description("Not use Auto Log if inventory is empty.")
			.defaultValue(true)
			.build()
	);

	private final Setting<List<Item>> ignoredItems = sgGeneral.add(new ItemListSetting.Builder()
			.name("ignores-items")
			.description("Items which ignores in inventory.")
			.defaultValue(List.of(Items.WOODEN_SWORD))
			.visible(ignoreIfNaked::get)
			.build()
	);


	private final Setting<Boolean> logMessagesActive = sgLogMessages.add(new BoolSetting.Builder()
			.name("log-messages")
			.description("Show log information after quit.")
			.defaultValue(true)
			.build()
	);

	private final Setting<Boolean> showNearbyPlayers = sgLogMessages.add(new BoolSetting.Builder()
			.name("show-nearby-players")
			.defaultValue(true)
			.build()
	);

	private final Setting<Integer> nearbyPlayerRange = sgLogMessages.add(new IntSetting.Builder()
			.name("nearby-players-range")
			.description("Maximum distance.")
			.defaultValue(30)
			.min(0)
			.sliderRange(5, 40)
			.visible(showNearbyPlayers::get)
			.build()
	);


	private final Setting<Boolean> ignoreFriends = sgLogMessages.add(new BoolSetting.Builder()
			.name("ignore-friends")
			.defaultValue(true)
			.build()
	);

	private final List<String> nearbyPlayers = new ArrayList<>();

	@Override
	public void onTickPre() {
		if (mc.player == null || mc.world == null || mc.player.isCreative() || mc.player.isSpectator())
			return;

		if (ignoreIfNaked.get() && InvUtility.isInventoryEmpty(ignoredItems.get()))
			return;

		int playerHealth = (int) mc.player.getHealth();

		if (showNearbyPlayers.get())
			collectNearbyPlayers();

		//        double lastTick = TickRate.INSTANCE.getTimeSinceLastTick();
		//
		//        if (lagCheck.get() && lastTick >= tickTime.get()) {
		//            _log(TextUtil.createText(new TextUtil.TextSettings("Server was lagged.", new Color(0x8C1A78), true)).append(concNearbyPlayers()));
		//            return;
		//        }

		switch (triggerMode.get()) {
			case Health -> {
				if (!noTotemsOnly.get() || !InvUtility.itemContains(Items.TOTEM_OF_UNDYING, true)) {
					if (playerHealth <= health.get()) {
						MutableText
								text =
								TextUtil.createText(new TextUtil.TextSettings(
										"Health was lower than ",
										new Color(0xFF3636),
										true
								));
						text.append(TextUtil.createText(new TextUtil.TextSettings(
								health.get() + " ",
								new Color(0xA381FF),
								true,
								false,
								true,
								false,
								false
						)));
						text.append(TextUtil.createText(new TextUtil.TextSettings(
								"Left: ",
								new Color(0xB93B28),
								true
						)));
						text.append(TextUtil.createText(new TextUtil.TextSettings(
								playerHealth + " ",
								new Color(0xDF8BFF),
								true,
								false,
								true,
								false,
								false
						)));
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
							.append(
									TextUtil
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
			if (player == mc.player)
				continue;

			int nearbyRange = nearbyPlayerRange.get();
			if (player.squaredDistanceTo(mc.player) > nearbyRange * nearbyRange)
				continue;

			if (ignoreFriends.get() && Friends.getInstance().isFriend(player))
				continue;

			nearbyPlayers.add(player.getName().getString());
		}
	}

	private Text concNearbyPlayers() {
		if (!showNearbyPlayers.get() || nearbyPlayers.isEmpty())
			return Text.empty();

		MutableText
				text =
				TextUtil.createText(new TextUtil.TextSettings("\nNearby players:\n", new Color(0x5A2AFF), true));
		text.append(TextUtil.createText(new TextUtil.TextSettings(nearbyPlayers + ".")));

		return text;
	}

	private void _log(Text message) {
		if (ignoreIfNaked.get() && InvUtility.isInventoryEmpty(ignoredItems.get()))
			return;

		log(message);

		if (toggleOff.get())
			disable();
	}

	public void log(Text message) {
		assert mc.player != null && mc.world != null;

		AutoReconnect autoReconnect = ModuleManager.getInstance().get(AutoReconnect.class);
		autoReconnect.disable();

		Screen screen = createScreen(message);

		switch (logMode.get()) {
			case Kick -> {
				//                Objects.requireNonNull(mc.getNetworkHandler()).sendCommand(kickCommand.get());
				warning("AutoLog triggered in Kick mode!");
				//                MeteorClient.EVENT_BUS.subscribe(new ScreenListener(message, mc));
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

	//    private record ScreenListener(Text message, MinecraftClient mc) {
	//
	//        @EventHandler
	//        private void onTick(TickEvent.Pre event) {
	//            if (mc.currentScreen == null)
	//                return;
	//
	//            if (mc.currentScreen instanceof HandledScreen<?>)
	//                return;
	//
	//            mc.setScreen(createScreen(message));
	//            deactivate();
	//        }
	//
	//        private void deactivate() {
	//            MeteorClient.EVENT_BUS.unsubscribe(this);
	//        }
	//    }
}
