package org.duollectis.client.api.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.settings.ColorSetting;
import org.duollectis.client.settings.EnumSetting;
import org.duollectis.client.settings.Setting;
import org.duollectis.client.settings.SettingGroup;

public class ModulesGui {

	public static void open() {
		MinecraftClient client = MinecraftClient.getInstance();

		// 1. Создаем основу экрана
		ConfigBuilder builder = ConfigBuilder
				.create()
				.setParentScreen(client.currentScreen)
				.setTitle(Text.literal("Modules"))
				.setSavingRunnable(() -> ModuleManager.getInstance().save());

		// 2. Создаем вкладку
		ConfigCategory category = builder.getOrCreateCategory(Text.literal("Functions"));
		ConfigEntryBuilder entryBuilder = builder.entryBuilder();


		for (Module module : ModuleManager.getInstance().all()) {

			category.addEntry(entryBuilder
					.startBooleanToggle(Text.literal(module.getTitle()), module.isActive())
					.setDefaultValue(false)
					.setSaveConsumer(module::toggle)
					.build());


			// 2. Генерация настроек Метеора
			for (SettingGroup group : module.getSettings()) {
				for (Setting<?> setting : group) {

					// Если настройка скрыта по логике (например, visible() вернул false) — пропускаем её рендер
					if (!setting.isVisible())
						continue;

					// Создаем форматированное имя с подсказкой из описания
					Text settingTitle = Text.literal("  └ [" + group.name + "] " + setting.title);
					Text settingTooltip = Text.literal(setting.description);

					// --- Обработка BOOLEAN настроек ---
					if (setting.get() instanceof Boolean) {
						Setting<Boolean> boolSetting = (Setting<Boolean>) setting;
						category.addEntry(entryBuilder
								.startBooleanToggle(settingTitle, boolSetting.get())
								.setDefaultValue(boolSetting.getDefaultValue())
								.setTooltip(settingTooltip)
								.setSaveConsumer(boolSetting::set) // Метод .set() сам вызовет onChanged()
								.build());
					}

					// --- Обработка INTEGER настроек (Числа и Цвета) ---
					else if (setting.get() instanceof Integer) {
						Setting<Integer> intSetting = (Setting<Integer>) setting;

						// Если в имени есть "color", Cloth Config автоматически выведет цветовую палитру
						if (setting.name.toLowerCase().contains("color") || setting.title
								.toLowerCase()
								.contains("цвет")) {
							category.addEntry(entryBuilder
									.startColorField(settingTitle, intSetting.get())
									.setDefaultValue(intSetting.getDefaultValue())
									.setTooltip(settingTooltip)
									.setSaveConsumer(intSetting::set)
									.build());
						}
						else {
							// Обычное числовое поле
							category.addEntry(entryBuilder
									.startIntField(settingTitle, intSetting.get())
									.setDefaultValue(intSetting.getDefaultValue())
									.setTooltip(settingTooltip)
									.setSaveConsumer(intSetting::set)
									.build());
						}
					}

					// --- Обработка DOUBLE/FLOAT настроек (например, дистанция атаки 4.5 блока) ---
					else if (setting.get() instanceof Double) {
						Setting<Double> doubleSetting = (Setting<Double>) setting;
						category.addEntry(entryBuilder
								.startDoubleField(settingTitle, doubleSetting.get())
								.setDefaultValue(doubleSetting.getDefaultValue())
								.setTooltip(settingTooltip)
								.setSaveConsumer(doubleSetting::set)
								.build());
					}
					else if (setting instanceof ColorSetting) {
						Setting<java.awt.Color> colorSetting = (Setting<java.awt.Color>) setting;
						category.addEntry(entryBuilder
								.startColorField(settingTitle, colorSetting.get().getRGB() & 0xFFFFFF) // Трэчит только RGB для палитры
								.setDefaultValue(colorSetting.getDefaultValue().getRGB() & 0xFFFFFF)
								.setTooltip(settingTooltip)
								.setSaveConsumer(newRgb -> {
									// Сохраняем обратно с сохранением дефолтной альфы (или вытаскиваем альфу, если нужно)
									colorSetting.set(new java.awt.Color(newRgb | 0xFF000000, true));
								})
								.build());
					}

					// --- Обработка ENUM настроек (наш новый EnumSetting) ---
					else if (setting instanceof EnumSetting) {
						EnumSetting<Enum<?>> enumSetting = (EnumSetting<Enum<?>>) setting;
						category.addEntry(entryBuilder
								.startEnumSelector(settingTitle, (Class<Enum<?>>) enumSetting.get().getClass(), enumSetting.get())
								.setDefaultValue(enumSetting.getDefaultValue())
								.setTooltip(settingTooltip)
								.setSaveConsumer(enumSetting::set)
								.build());
					}

					// Сюда по аналогии будут добавляться StringSetting, EnumSetting и др.
				}
			}

		}

		client.setScreen(builder.build());
	}
}