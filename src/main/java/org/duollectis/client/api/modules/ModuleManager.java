package org.duollectis.client.api.modules;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.duollectis.client.DuollectisMod;
import org.duollectis.client.api.event.EventManager;
import org.duollectis.client.modules.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class ModuleManager extends EventManager<Module> {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.setFormattingStyle(FormattingStyle.PRETTY.withIndent("    "))
			.serializeNulls()
			.create();

	@Getter
	private static final ModuleManager instance = new ModuleManager();

	private final Map<Class<? extends Module>, Module> instanceMap = new Reference2ReferenceOpenHashMap<>();

	private ModuleManager() {
		add(new ZombieDuper());
		add(new AutoGap());
		add(new AutoLog());
		add(new AutoReconnect());
		add(new TestModule());

		load();
	}

	public void disableAll() {
		all().forEach(Module::disable);
	}

	public JsonObject toTag() {
		JsonObject tag = new JsonObject();

		JsonArray modulesTag = new JsonArray();

		for (Module module : all()) {
			JsonObject moduleTag = module.toTag();
			if (moduleTag != null)
				modulesTag.add(moduleTag);
		}

		tag.add("modules", modulesTag);

		return tag;
	}


	public void save() {

		File minecraftDir = FabricLoader.getInstance().getGameDir().toFile();

		File file = new File(minecraftDir, "duollectis/settings.json");

		JsonObject tag = toTag();

		System.out.println(tag);

		if (tag == null)
			return;

		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			GSON.toJson(tag, writer);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load() {
		File minecraftDir = FabricLoader.getInstance().getGameDir().toFile();

		File file = new File(minecraftDir, "duollectis/settings.json");

		if (!file.exists()) {
			return;
		}

		try (FileReader reader = new FileReader(file)) {

			JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();

			disableAll();

			for (JsonElement moduleTagI : object.get("modules").getAsJsonArray()) {
				JsonObject moduleTag = moduleTagI.getAsJsonObject();

				Module module = get(moduleTag.get("name").getAsString());

				if (module != null) {
					module.fromTag(moduleTag);
				}
			}

		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	public Collection<Module> all() {
		return childs;
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T get(Class<T> clazz) {
		return (T) instanceMap.get(clazz);
	}

	public Module get(String name) {
		for (Module module : all()) {
			if (module.getName().equalsIgnoreCase(name))
				return module;
		}

		return null;
	}

	@Override
	public Module add(Module module) {
		ModuleInfo info = module.getClass().getAnnotation(ModuleInfo.class);
		module.setName(info.name());
		module.setDescription(info.description());

		instanceMap.put(module.getClass(), module);
		return super.add(module);
	}
}
