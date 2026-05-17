package org.duollectis.client.api.menu;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.settings.ColorSetting;
import org.duollectis.client.settings.EnumSetting;
import org.duollectis.client.settings.Setting;
import org.duollectis.client.settings.SettingGroup;

public class ModulesMenu {

	public static void open() {
		MinecraftClient client = MinecraftClient.getInstance();

		ConfigBuilder builder = ConfigBuilder
				.create()
				.setParentScreen(client.currentScreen)
				.setTitle(Text.literal("Modules"))
				.setSavingRunnable(() -> ModuleManager.getInstance().save());

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();


		for (Module module : ModuleManager.getInstance().all()) {
			ConfigCategory category = builder.getOrCreateCategory(Text.literal(module.getTitle()));

			category.addEntry(entryBuilder
					.startBooleanToggle(Text.literal("Enabled"), module.isActive())
					.setTooltip(Text.literal(module.getDescription()))
					.setDefaultValue(false)
					.setSaveConsumer(module::toggle)
					.build());

			// 2. Генерация настроек Метеора
			for (SettingGroup group : module.getSettings()) {

				SubCategoryBuilder subCategory = entryBuilder
						.startSubCategory(Text.literal(group.name))
						.setExpanded(true);

				for (Setting<?> setting : group) {

					// Если настройка скрыта по логике (например, visible() вернул false) — пропускаем её рендер
					//					if (!setting.isVisible())
					//						continue;

					Text settingTitle = Text.literal(setting.title);
					Text settingTooltip = Text.literal(setting.description);

					// --- Обработка BOOLEAN настроек ---
					if (setting.get() instanceof Boolean) {
						Setting<Boolean> boolSetting = (Setting<Boolean>) setting;
						subCategory.add(entryBuilder
								.startBooleanToggle(settingTitle, boolSetting.get())
								.setDefaultValue(boolSetting.getDefaultValue())
								.setTooltip(settingTooltip)
								.setSaveConsumer(boolSetting::set) // Метод .set() сам вызовет onChanged()
								.build());
					}

					// --- Обработка INTEGER настроек (Числа и Цвета) ---
					else if (setting.get() instanceof Integer) {
						Setting<Integer> intSetting = (Setting<Integer>) setting;

						// Обычное числовое поле
						subCategory.add(entryBuilder
								.startIntField(settingTitle, intSetting.get())
								.setDefaultValue(intSetting.getDefaultValue())
								.setTooltip(settingTooltip)
								.setSaveConsumer(intSetting::set)
								.build());
					}

					// --- Обработка DOUBLE/FLOAT настроек (например, дистанция атаки 4.5 блока) ---
					else if (setting.get() instanceof Double) {
						Setting<Double> doubleSetting = (Setting<Double>) setting;
						subCategory.add(entryBuilder
								.startDoubleField(settingTitle, doubleSetting.get())
								.setDefaultValue(doubleSetting.getDefaultValue())
								.setTooltip(settingTooltip)
								.setSaveConsumer(doubleSetting::set)
								.build());
					}
					else if (setting instanceof ColorSetting) {
						Setting<java.awt.Color> colorSetting = (Setting<java.awt.Color>) setting;
						subCategory.add(entryBuilder
								.startColorField(
										settingTitle,
										colorSetting.get().getRGB() & 0xFFFFFF
								) // Трэчит только RGB для палитры
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
						subCategory.add(entryBuilder
								.startEnumSelector(
										settingTitle,
										(Class<Enum<?>>) enumSetting.get().getClass(),
										enumSetting.get()
								)
								.setDefaultValue(enumSetting.getDefaultValue())
								.setTooltip(settingTooltip)
								.setSaveConsumer(enumSetting::set)
								.build());
					}

					// Сюда по аналогии будут добавляться StringSetting, EnumSetting и др.
				}

				category.addEntry(subCategory.build());
			}

		}

		client.setScreen(builder.build());
	}
}