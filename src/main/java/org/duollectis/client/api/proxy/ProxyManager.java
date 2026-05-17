package org.duollectis.client.api.proxy;

import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import org.duollectis.client.utils.JsonHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProxyManager {

	@Getter
	private static ProxyManager instance;

	private final List<Proxy> proxies = load();

	@Getter
	private Proxy activeProxy;

	public ProxyManager() {
		instance = this;
	}

	private static List<Proxy> load() {
		File minecraftDir = FabricLoader.getInstance().getGameDir().toFile();
		File file = new File(minecraftDir, "duollectis/proxies.json");

		if (file.exists()) {

			try (FileReader fileReader = new FileReader(file)) {
				return JsonHelper.GSON.fromJson(fileReader, new TypeToken<>() {});
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		return new ArrayList<>();
	}

}
