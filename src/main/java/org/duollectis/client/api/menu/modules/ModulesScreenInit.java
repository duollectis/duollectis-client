package org.duollectis.client.api.menu.modules;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.i18n.ClientConfig;
import org.duollectis.client.api.i18n.ClientLocale;
import org.duollectis.client.api.menu.widget.BoolToggleWidget;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;
import org.duollectis.client.api.menu.widget.ItemListWidget;
import org.duollectis.client.api.menu.widget.NumericInputWidget;
import org.duollectis.client.api.menu.widget.StyledSliderWidget;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.*;
import org.duollectis.client.utils.INamed;
import org.duollectis.client.utils.color.Color;

import java.util.List;
import java.util.function.Consumer;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

/**
 * Утилита инициализации виджетов экрана модулей: создаёт слайдеры, поля ввода,
 * переключатели и выпадающие списки для каждой настройки выбранного модуля,
 * а также виджеты панели аккаунтов.
 */
final class ModulesScreenInit {

	private ModulesScreenInit() {}

	static void langDropdown(ModulesScreen screen) {
		int langY = screen.panelY + PANEL_H + PAD;
		int langX = screen.panelX + PANEL_W - LANG_FIELD_W - PAD;

		ClientLocale[] locales = ClientLocale.values();
		ClientLocale current = ClientConfig.getInstance().getLocale();

		EnumDropdownWidget<ClientLocale> dropdown = screen.register(new EnumDropdownWidget<>(
				screen.textRenderer(),
				langX,
				langY,
				LANG_FIELD_W,
				ROW_H,
				locales,
				current,
				ClientConfig.getInstance()::setLocale
		));

		screen.langDropdown = dropdown;
		screen.langDropdowns.add(dropdown);
	}

	static void accountWidgets(ModulesScreen screen) {
		int contentX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int listY = screen.panelY + PAD;
		int plusBtnW = 20;

		screen.accAddAccountBtn[0] = contentX;
		screen.accAddAccountBtn[1] = listY;
		screen.accAddAccountBtn[2] = plusBtnW;
		screen.accAddAccountBtn[3] = ACC_FIELD_H;

		screen.accAddGroupBtn[0] = contentX + plusBtnW + ACC_ROW_GAP;
		screen.accAddGroupBtn[1] = listY;
		screen.accAddGroupBtn[2] = plusBtnW;
		screen.accAddGroupBtn[3] = ACC_FIELD_H;
	}

	static void settingsWidgets(ModulesScreen screen) {
		if (screen.selectedModule == null) {
			return;
		}

		int settingsX = screen.panelX + LIST_W + DIVIDER_W + PAD;
		int settingsW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
		int panelTop = screen.panelY + PAD + ROW_H;
		int panelBottom = screen.panelY + PANEL_H - PAD;
		int y = panelTop - screen.settingsScroll;
		int dividerH = SETTING_DIVIDER_PAD * 2 + 1;

		for (SettingGroup group : screen.selectedModule.getSettings().getGroups()) {
			y += ROW_H;

			List<Setting<?>> visibleSettings = group.settings()
			                                        .stream()
			                                        .filter(Setting::isVisible)
			                                        .toList();

			for (int i = 0; i < visibleSettings.size(); i++) {
				Setting<?> setting = visibleSettings.get(i);
				int itemH = ModulesScreenRenderer.itemHeight(screen.textRenderer(), setting);
				int nameH = screen.textRenderer().fontHeight;
				int top = ModulesScreenRenderer.offsetTop(setting);
				int gap = ModulesScreenRenderer.nameGap(setting);
				boolean inline = setting instanceof BoolSetting || setting instanceof EnumSetting;
				int nameY = y + top;
				int controlY = inline ? nameY : nameY + nameH + gap;

				if (controlY + ROW_H > panelTop && y < panelBottom) {
					buildWidget(screen, setting, settingsX, nameY, controlY, settingsW);
				}

				y += itemH;

				if (i < visibleSettings.size() - 1) {
					y += dividerH;
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static void buildWidget(
			ModulesScreen screen,
			Setting<?> setting,
			int x,
			int nameY,
			int controlY,
			int w
	) {
		if (setting instanceof BoolSetting boolSetting) {
			BoolToggleWidget toggle = new BoolToggleWidget(
					screen.textRenderer(), x, controlY, w, ROW_H,
					setting.getName(), boolSetting.get(), boolSetting::set
			);

			screen.boolToggles.add(toggle);
			screen.register(toggle);
			return;
		}

		if (setting instanceof IntSetting intSetting) {
			buildIntWidget(screen, intSetting, x, nameY, controlY, w);
			return;
		}

		if (setting instanceof DoubleSetting doubleSetting) {
			buildDoubleWidget(screen, doubleSetting, x, nameY, controlY, w);
			return;
		}

		if (setting instanceof ColorSetting colorSetting) {
			registerPlainTextField(
					screen,
					x, controlY, w,
					String.format("%08X", colorSetting.get().getPacked()),
					text -> {
						try {
							colorSetting.set(new Color((int) Long.parseLong(text.trim(), 16)));
						}
						catch (NumberFormatException ignored) {
						}
					}
			);
			return;
		}

		if (setting instanceof EnumSetting enumSetting) {
			Enum<?> current = (Enum<?>) enumSetting.get();
			Enum<?>[] values = current.getDeclaringClass().getEnumConstants();
			int dropW = computeEnumDropdownWidth(screen, values);
			EnumDropdownWidget<?> dropdown = buildEnumDropdown(
					screen, x + w - dropW, controlY, dropW, values, current, enumSetting
			);
			screen.dropdowns.add(dropdown);
			screen.register(dropdown);
			return;
		}

		if (setting instanceof ItemListSetting itemListSetting) {
			ItemListWidget widget = new ItemListWidget(
					screen.textRenderer(), x, controlY, w, ROW_H, itemListSetting
			);
			screen.itemListWidgets.add(widget);
		}
	}

	private static void buildIntWidget(
			ModulesScreen screen,
			IntSetting intSetting,
			int x,
			int nameY,
			int controlY,
			int w
	) {
		if (intSetting.isSlider()) {
			int inputW = computeInputWidth(screen, String.valueOf(intSetting.get()), intSetting.getSuffix());

			NumericInputWidget input = new NumericInputWidget(
					screen.textRenderer(),
					x + w - inputW, nameY, inputW, ROW_H,
					w / 2,
					intSetting.getMin(), intSetting.getMax(),
					true, 0,
					intSetting.getSuffix(),
					String.valueOf(intSetting.get()),
					value -> intSetting.set((int) Math.round(value))
			);

			StyledSliderWidget slider = new StyledSliderWidget(
					screen.textRenderer(), x, controlY, w, ROW_H,
					intSetting.getSliderMin(), intSetting.getSliderMax(), intSetting.get(),
					0, value -> intSetting.set((int) Math.round(value))
			);

			slider.setOnChange(value -> {
				int rounded = (int) Math.round(value);
				intSetting.set(rounded);
				input.updateDisplay(rounded);
			});

			registerSliderWithInput(screen, slider, input);
			return;
		}

		registerPlainTextField(
				screen, x, controlY, w,
				String.valueOf(intSetting.get()),
				text -> {
					try {
						intSetting.set(Integer.parseInt(text.trim()));
					}
					catch (NumberFormatException ignored) {
					}
				}
		);
	}

	private static void buildDoubleWidget(
			ModulesScreen screen,
			DoubleSetting doubleSetting,
			int x,
			int nameY,
			int controlY,
			int w
	) {
		if (doubleSetting.isSlider()) {
			int decimals = doubleSetting.getDecimalPlaces();
			String formatted = String.format("%." + decimals + "f", doubleSetting.get());
			int inputW = computeInputWidth(screen, formatted, doubleSetting.getSuffix());

			NumericInputWidget input = new NumericInputWidget(
					screen.textRenderer(),
					x + w - inputW, nameY, inputW, ROW_H,
					w / 2,
					doubleSetting.getMin(), doubleSetting.getMax(),
					false, decimals,
					doubleSetting.getSuffix(),
					formatted,
					doubleSetting::set
			);

			StyledSliderWidget slider = new StyledSliderWidget(
					screen.textRenderer(), x, controlY, w, ROW_H,
					doubleSetting.getSliderMin(), doubleSetting.getSliderMax(), doubleSetting.get(),
					decimals, doubleSetting::set
			);

			slider.setOnChange(value -> {
				doubleSetting.set(value.doubleValue());
				input.updateDisplay(value.doubleValue());
			});

			registerSliderWithInput(screen, slider, input);
			return;
		}

		registerPlainTextField(
				screen, x, controlY, w,
				String.valueOf(doubleSetting.get()),
				text -> {
					try {
						doubleSetting.set(Double.parseDouble(text.trim()));
					}
					catch (NumberFormatException ignored) {
					}
				}
		);
	}

	private static void registerSliderWithInput(
			ModulesScreen screen,
			StyledSliderWidget slider,
			NumericInputWidget input
	) {
		input.linkSlider(slider);
		input.setOnBlur(() -> screen.setFocused(null));

		screen.sliders.add(slider);
		screen.register(slider);
		screen.textFields.add(input);
		screen.register(input);
	}

	private static void registerPlainTextField(
			ModulesScreen screen,
			int x,
			int y,
			int w,
			String initialText,
			Consumer<String> onChange
	) {
		TextFieldWidget field = new TextFieldWidget(screen.textRenderer(), x, y, w, ROW_H, Text.empty());
		field.setText(initialText);
		field.setChangedListener(onChange);
		field.setDrawsBackground(false);
		screen.textFields.add(field);
		screen.register(field);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <E extends Enum<E>> EnumDropdownWidget<E> buildEnumDropdown(
			ModulesScreen screen,
			int x,
			int y,
			int w,
			Enum<?>[] rawValues,
			Enum<?> rawInitial,
			EnumSetting rawSetting
	) {
		E[] values = (E[]) rawValues;
		E initial = (E) rawInitial;
		return new EnumDropdownWidget<>(
				screen.textRenderer(), x, y, w, ROW_H,
				values, initial,
				(Consumer<E>) rawSetting::set
		);
	}

	private static int computeInputWidth(ModulesScreen screen, String number, String suffix) {
		String display = suffix.isEmpty() ? number : number + " " + suffix;
		return screen.textRenderer().getWidth(display) + 8;
	}

	private static int computeEnumDropdownWidth(ModulesScreen screen, Enum<?>[] values) {
		int maxTextW = 0;

		for (Enum<?> value : values) {
			String label = value instanceof INamed named ? named.getName() : value.name();
			int textW = screen.textRenderer().getWidth("● " + label);
			maxTextW = Math.max(maxTextW, textW);
		}

		return maxTextW + 8;
	}
}
