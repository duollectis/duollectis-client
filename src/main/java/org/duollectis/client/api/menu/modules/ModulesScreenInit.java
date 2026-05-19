package org.duollectis.client.api.menu.modules;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.i18n.ClientConfig;
import org.duollectis.client.api.i18n.ClientLocale;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;
import org.duollectis.client.api.menu.widget.StyledSliderWidget;
import org.duollectis.client.api.menu.widget.StyledToggleButton;
import org.duollectis.client.settings.BoolSetting;
import org.duollectis.client.settings.ColorSetting;
import org.duollectis.client.settings.DoubleSetting;
import org.duollectis.client.settings.EnumSetting;
import org.duollectis.client.settings.IntSetting;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.SettingGroup;
import org.duollectis.client.utils.color.Color;

import java.util.function.Consumer;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

final class ModulesScreenInit {

    private ModulesScreenInit() {}

    static void langDropdown(ModulesScreen screen) {
        int langY = screen.panelY + PANEL_H + PAD;
        int langX = screen.panelX + PANEL_W - LANG_FIELD_W - PAD;

        ClientLocale[] locales = ClientLocale.values();
        ClientLocale current = ClientConfig.getInstance().getLocale();

        EnumDropdownWidget<ClientLocale> dropdown = new EnumDropdownWidget<>(
            screen.textRenderer(),
            langX,
            langY,
            LANG_FIELD_W,
            ROW_H,
            locales,
            current,
            ClientConfig.getInstance()::setLocale
        );

        screen.langDropdown = dropdown;
        screen.langDropdowns.add(dropdown);
        screen.register(dropdown);
    }

    static void settingsWidgets(ModulesScreen screen) {
        if (screen.selectedModule == null) {
            return;
        }

        int settingsX = screen.panelX + LIST_W + DIVIDER_W + PAD;
        int settingsW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
        int sliderW = settingsW - SLIDER_INPUT_W - PAD;

        int panelTop = screen.panelY + PAD + ROW_H + ROW_GAP;
        int panelBottom = screen.panelY + PANEL_H - PAD;
        int y = panelTop - screen.settingsScroll;

        for (SettingGroup group : screen.selectedModule.getSettings().getGroups()) {
            y += ROW_H + ROW_GAP;

            for (Setting<?> setting : group.settings()) {
                if (setting.isVisible() == false) {
                    continue;
                }

                int controlY = y + LABEL_H;

                if (controlY + ROW_H > panelTop && y < panelBottom) {
                    buildWidget(screen, setting, settingsX, controlY, settingsW, sliderW);
                }

                y += SETTING_H + ROW_GAP;
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void buildWidget(
            ModulesScreen screen,
            Setting<?> setting,
            int x,
            int controlY,
            int w,
            int sliderW
    ) {
        if (setting instanceof BoolSetting boolSetting) {
            StyledToggleButton toggle = new StyledToggleButton(
                screen.textRenderer(), x, controlY, w, ROW_H,
                boolSetting.get(), boolSetting::set
            );
            screen.toggles.add(toggle);
            screen.register(toggle);
            return;
        }

        if (setting instanceof IntSetting intSetting) {
            buildIntWidget(screen, intSetting, x, controlY, w, sliderW);
            return;
        }

        if (setting instanceof DoubleSetting doubleSetting) {
            buildDoubleWidget(screen, doubleSetting, x, controlY, w, sliderW);
            return;
        }

        if (setting instanceof ColorSetting colorSetting) {
            TextFieldWidget field = styledTextField(
                screen,
                x, controlY, w,
                String.format("%08X", colorSetting.get().getPacked()),
                text -> {
                    try {
                        colorSetting.set(new Color((int) Long.parseLong(text.trim(), 16)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            );
            screen.textFields.add(field);
            screen.register(field);
            return;
        }

        if (setting instanceof EnumSetting enumSetting) {
            Enum<?> current = (Enum<?>) enumSetting.get();
            Enum<?>[] values = current.getDeclaringClass().getEnumConstants();
            EnumDropdownWidget<?> dropdown = buildEnumDropdown(
                screen, x, controlY, w, values, current, enumSetting
            );
            screen.dropdowns.add(dropdown);
            screen.register(dropdown);
        }
    }

    private static void buildIntWidget(
            ModulesScreen screen,
            IntSetting intSetting,
            int x,
            int controlY,
            int w,
            int sliderW
    ) {
        if (intSetting.isNoSlider()) {
            TextFieldWidget field = styledTextField(
                screen,
                x, controlY, w,
                String.valueOf(intSetting.get()),
                text -> {
                    try {
                        intSetting.set(Integer.parseInt(text.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            );
            screen.textFields.add(field);
            screen.register(field);
            return;
        }

        int inputX = x + sliderW + PAD;

        StyledSliderWidget slider = new StyledSliderWidget(
            screen.textRenderer(), x, controlY, sliderW, ROW_H,
            intSetting.getSliderMin(), intSetting.getSliderMax(), intSetting.get(),
            0, value -> intSetting.set((int) Math.round(value))
        );

        TextFieldWidget input = styledTextField(
            screen,
            inputX, controlY, SLIDER_INPUT_W,
            String.valueOf(intSetting.get()),
            text -> {
                try {
                    int parsed = Integer.parseInt(text.trim());
                    intSetting.set(parsed);
                    slider.setValue(parsed);
                } catch (NumberFormatException ignored) {
                }
            }
        );

        slider.setOnChange(value -> {
            int rounded = (int) Math.round(value.doubleValue());
            intSetting.set(rounded);
            input.setText(String.valueOf(rounded));
        });

        screen.sliders.add(slider);
        screen.register(slider);
        screen.textFields.add(input);
        screen.register(input);
    }

    private static void buildDoubleWidget(
            ModulesScreen screen,
            DoubleSetting doubleSetting,
            int x,
            int controlY,
            int w,
            int sliderW
    ) {
        if (doubleSetting.isNoSlider()) {
            TextFieldWidget field = styledTextField(
                screen,
                x, controlY, w,
                String.valueOf(doubleSetting.get()),
                text -> {
                    try {
                        doubleSetting.set(Double.parseDouble(text.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            );
            screen.textFields.add(field);
            screen.register(field);
            return;
        }

        int inputX = x + sliderW + PAD;
        int decimals = doubleSetting.getDecimalPlaces();

        StyledSliderWidget slider = new StyledSliderWidget(
            screen.textRenderer(), x, controlY, sliderW, ROW_H,
            doubleSetting.getSliderMin(), doubleSetting.getSliderMax(), doubleSetting.get(),
            decimals, value -> doubleSetting.set(value)
        );

        TextFieldWidget input = styledTextField(
            screen,
            inputX, controlY, SLIDER_INPUT_W,
            String.format("%." + decimals + "f", doubleSetting.get()),
            text -> {
                try {
                    double parsed = Double.parseDouble(text.trim());
                    doubleSetting.set(parsed);
                    slider.setValue(parsed);
                } catch (NumberFormatException ignored) {
                }
            }
        );

        slider.setOnChange(value -> {
            doubleSetting.set(value.doubleValue());
            input.setText(String.format("%." + decimals + "f", value.doubleValue()));
        });

        screen.sliders.add(slider);
        screen.register(slider);
        screen.textFields.add(input);
        screen.register(input);
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

    private static TextFieldWidget styledTextField(
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
        return field;
    }

}
