package org.duollectis.client.api.proxy;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class ProxyManager {

    @Getter
    private static ProxyManager instance;

    private final Set<Proxy> proxiesSet = new LinkedHashSet<>();

    @Setter
    @Getter
    private Proxy activeProxy;

    private static final Gson PROXY_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Proxy.Type.class, new ProxyTypeAdapter())
            .registerTypeHierarchyAdapter(EnumSet.class, new ProxyTypeSetAdapter())
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

    public void addProxies(Collection<Proxy> proxies) {
        proxiesSet.addAll(proxies);
    }

    public void removeProxy(Proxy proxy) {
        if (activeProxy == proxy) {
            activeProxy = null;
        }
        proxiesSet.remove(proxy);
    }

    public void save() {
        File file = proxyFile();
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            PROXY_GSON.toJson(new ArrayList<>(proxiesSet), PROXY_LIST_TYPE, writer);
        } catch (IOException e) {
            log.error("Не удалось сохранить список прокси", e);
        }
    }

    private void load() {
        File file = proxyFile();

        if (file.exists() == false) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            List<Proxy> loaded = PROXY_GSON.fromJson(reader, PROXY_LIST_TYPE);

            if (loaded == null) {
                return;
            }

            for (Proxy proxy : loaded) {
                proxy.initRuntimeFields();
                proxiesSet.add(proxy);
            }
        } catch (IOException e) {
            log.error("Не удалось загрузить список прокси", e);
        }
    }

    private static File proxyFile() {
        return new File(FabricLoader.getInstance().getGameDir().toFile(), "duollectis/proxies.json");
    }

    private static final class ProxyTypeAdapter
            implements JsonSerializer<Proxy.Type>, JsonDeserializer<Proxy.Type> {

        @Override
        public JsonElement serialize(Proxy.Type src, Type typeOfSrc, JsonSerializationContext ctx) {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.name());
        }

        @Override
        public Proxy.Type deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            try {
                return Proxy.Type.valueOf(json.getAsString());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static final class ProxyTypeSetAdapter
            implements JsonSerializer<EnumSet<Proxy.Type>>, JsonDeserializer<EnumSet<Proxy.Type>> {

        @Override
        public JsonElement serialize(EnumSet<Proxy.Type> src, Type typeOfSrc, JsonSerializationContext ctx) {
            if (src == null || src.isEmpty()) {
                return new JsonArray();
            }
            JsonArray array = new JsonArray();
            for (Proxy.Type type : src) {
                array.add(type.name());
            }
            return array;
        }

        @Override
        public EnumSet<Proxy.Type> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            EnumSet<Proxy.Type> result = EnumSet.noneOf(Proxy.Type.class);
            if (json == null || json.isJsonNull() || !json.isJsonArray()) {
                return result;
            }
            for (JsonElement element : json.getAsJsonArray()) {
                try {
                    result.add(Proxy.Type.valueOf(element.getAsString()));
                } catch (IllegalArgumentException ignored) {}
            }
            return result;
        }
    }
}
