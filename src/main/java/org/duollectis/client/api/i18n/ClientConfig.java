package org.duollectis.client.api.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import org.duollectis.client.utils.JsonHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public final class ClientConfig {

    private static final ClientLocale DEFAULT_LOCALE = ClientLocale.EN_US;
    private static final String CONFIG_FILE = "duollectis/client-config.json";
    private static final String KEY_LOCALE = "locale";

    @Getter
    private static final ClientConfig instance = new ClientConfig();

    @Getter
    private ClientLocale locale = DEFAULT_LOCALE;

    private ClientConfig() {
        load();
    }

    public void setLocale(ClientLocale newLocale) {
        locale = newLocale;
        ClientTranslations.getInstance().reload(newLocale);
        save();
    }

    private void load() {
        File file = resolveFile();

        if (file.exists() == false) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has(KEY_LOCALE)) {
                String savedFileName = root.get(KEY_LOCALE).getAsString();
                locale = Arrays.stream(ClientLocale.values())
                    .filter(l -> l.getFileName().equals(savedFileName))
                    .findFirst()
                    .orElse(DEFAULT_LOCALE);
            }
        } catch (Exception e) {
            log.error("Не удалось загрузить конфиг клиента", e);
        }
    }

    public void save() {
        File file = resolveFile();
        file.getParentFile().mkdirs();

        JsonObject root = new JsonObject();
        root.addProperty(KEY_LOCALE, locale.getFileName());

        try (FileWriter writer = new FileWriter(file)) {
            JsonHelper.GSON.toJson(root, writer);
        } catch (IOException e) {
            log.error("Не удалось сохранить конфиг клиента", e);
        }
    }

    private static File resolveFile() {
        return FabricLoader.getInstance().getGameDir().resolve(CONFIG_FILE).toFile();
    }

}
