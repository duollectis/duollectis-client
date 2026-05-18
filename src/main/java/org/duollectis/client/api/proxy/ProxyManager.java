package org.duollectis.client.api.proxy;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProxyManager {

	@Getter
	private static ProxyManager instance;

	/** Insertion-ordered set — deduplicates by address+port (Proxy.equals/hashCode). */
	private final Set<Proxy> proxiesSet = new LinkedHashSet<>();

	@Setter
	@Getter
	private Proxy activeProxy;

	/**
	 * Gson instance used exclusively for Proxy serialisation.
	 * - excludeFieldsWithoutExposeAnnotation() → only @Expose fields are written/read
	 * - registerTypeAdapter for Proxy.Type → null serialises as JSON null,
	 * JSON null / missing deserialises as null (type not yet determined)
	 */
	private static final Gson PROXY_GSON = new GsonBuilder()
			.setPrettyPrinting()
			.excludeFieldsWithoutExposeAnnotation()
			.registerTypeAdapter(Proxy.Type.class, new ProxyTypeAdapter())
			.create();

	private static final Type PROXY_LIST_TYPE = new TypeToken<List<Proxy>>() {}.getType();

	public ProxyManager() {
		instance = this;
		load();
	}

	/** Returns a snapshot list of all proxies (insertion order). */
	public List<Proxy> getProxies() {
		return new ArrayList<>(proxiesSet);
	}

	public void addProxy(Proxy proxy) {
		proxiesSet.add(proxy);
	}

	/**
	 * Add many proxies at once; caller is responsible for calling save().
	 */
	public void addProxies(Collection<Proxy> list) {
		proxiesSet.addAll(list);
	}

	public void removeProxy(Proxy proxy) {
		if (activeProxy == proxy)
			activeProxy = null;
		proxiesSet.remove(proxy);
	}

	public void save() {
		File file = proxyFile();
		file.getParentFile().mkdirs();
		List<Proxy> snapshot = new ArrayList<>(proxiesSet);
		try (FileWriter writer = new FileWriter(file)) {
			PROXY_GSON.toJson(snapshot, PROXY_LIST_TYPE, writer);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void load() {
		File file = proxyFile();
		if (!file.exists())
			return;
		try (FileReader reader = new FileReader(file)) {
			List<Proxy> loaded = PROXY_GSON.fromJson(reader, PROXY_LIST_TYPE);
			if (loaded != null) {
				for (Proxy p : loaded) {
					p.initRuntimeFields();  // restore volatile defaults after Unsafe construction
					proxiesSet.add(p);
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static File proxyFile() {
		return new File(FabricLoader.getInstance().getGameDir().toFile(), "duollectis/proxies.json");
	}

	// ── TypeAdapter for Proxy.Type ────────────────────────────────────────────
	// Serialises null as JSON null; deserialises JSON null / unknown string as null.
	private static class ProxyTypeAdapter implements JsonSerializer<Proxy.Type>, JsonDeserializer<Proxy.Type> {

		@Override
		public JsonElement serialize(Proxy.Type src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext ctx) {
			return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.name());
		}

		@Override
		public Proxy.Type deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext ctx)
		throws JsonParseException {
			if (json == null || json.isJsonNull())
				return null;
			String s = json.getAsString();
			try {
				return Proxy.Type.valueOf(s);
			}
			catch (IllegalArgumentException e) {
				return null; // "UNKNOWN" or any unrecognised string → null
			}
		}
	}
}
