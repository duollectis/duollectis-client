package org.duollectis.client.api.modules;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.duollectis.client.api.event.EventHandler;
import org.duollectis.client.settings.Settings;
import org.duollectis.client.utils.ISerializable;
import org.duollectis.client.utils.Utils;

public class Module implements EventHandler, ISerializable<Module> {

	public static final MinecraftClient mc = MinecraftClient.getInstance();

	@Getter
	protected final Settings settings = new Settings();

	@Getter
	@Setter
	private boolean active;

	@Getter
	private String name;

	@Getter
	private String title;

	@Getter
	@Setter
	private String description;

	protected boolean debug;

	public void setName(String name) {
		this.name = name;
		this.title = Utils.nameToTitle(name);
	}

	public void toggle() {
		if (active) {
			disable();
		}
		else {
			enable();
		}
	}

	public void toggle(boolean active) {
		if (active) {
			enable();
		}
		else {
			disable();
		}
	}

	public void enable() {
		if (!active) {
			active = true;
			onActivate();
			info("Activated");
		}
	}

	public void disable() {
		if (active) {
			active = false;
			onDeactivate();
			info("Deactivated");
		}
	}

	protected void onActivate() {}

	protected void onDeactivate() {}

	public void debugInfo(Object arg) {
		if (debug) {
			info(arg);
		}
	}

	public void info(Object arg) {
		addMessage(arg, 0x60FF60);
	}

	public void warning(Object arg) {
		addMessage(arg, 0xFFFF60);
	}

	public void error(Object arg) {
		addMessage(arg, 0xFF6060);
	}

	private static void addMessage(Object arg, int color) {
		if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null) {
			return;
		}

		mc.inGameHud.getChatHud().addMessage(Text
				.literal(String.valueOf(arg))
				.setStyle(Style.EMPTY.withColor(color))
		);
	}

	@Override
	public JsonObject toTag() {
		JsonObject tag = new JsonObject();

		tag.addProperty("name", name);
		//		tag.put("keybind", keybind.toTag());
		tag.add("settings", settings.toTag());
		tag.addProperty("active", active);

		return tag;
	}

	@Override
	public Module fromTag(JsonObject tag) {

		//		keybind.fromTag(tag.getCompoundOrEmpty("keybind"));

		JsonObject settingsTag = tag.get("settings").getAsJsonObject();

		settings.fromTag(settingsTag);

		boolean active = tag.has("active") && tag.get("active").getAsBoolean();

		if (active != isActive()) {
			toggle();
		}

		return this;
	}
}
