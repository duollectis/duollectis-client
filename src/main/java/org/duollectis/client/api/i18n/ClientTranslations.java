package org.duollectis.client.api.i18n;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class ClientTranslations {

    private static final ClientLocale FALLBACK_LOCALE = ClientLocale.EN_US;
    private static final String BUILTIN_LANG_PATH = "assets/duollectis/client/lang/";

    @Getter
    private static final ClientTranslations instance = new ClientTranslations();

    private final Map<String, String> translations = new HashMap<>();

    private ClientTranslations() {}

    public String translate(String key) {
        return translations.getOrDefault(key, key);
    }

    public void reload(ClientLocale locale) {
        translations.clear();

        loadFromBuiltin(FALLBACK_LOCALE);

        if (locale != FALLBACK_LOCALE) {
            loadFromBuiltin(locale);
        }
    }

    private void loadFromBuiltin(ClientLocale locale) {
        String resourcePath = BUILTIN_LANG_PATH + locale.getFileName() + ".json";
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (stream == null) {
            log.warn("Встроенный файл локали не найден: {}", resourcePath);
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            flattenInto(root, "", translations);
        } catch (Exception e) {
            log.error("Не удалось загрузить встроенный перевод: {}", locale.getFileName(), e);
        }
    }

    /**
     * Рекурсивно разворачивает вложенные JSON-объекты в плоскую карту.
     * {"a": {"b": {"c": "val"}}} → "a.b.c" = "val"
     */
    private static void flattenInto(JsonObject node, String prefix, Map<String, String> target) {
        for (Map.Entry<String, JsonElement> entry : node.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement value = entry.getValue();

            if (value.isJsonObject()) {
                flattenInto(value.getAsJsonObject(), key, target);
            } else if (value.isJsonPrimitive()) {
                target.put(key, value.getAsString());
            }
        }
    }

}
