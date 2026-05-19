package org.duollectis.client.api.menu.modules;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.settings.SettingGroup;

import java.util.List;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

@UtilityClass
public class ModulesScreenRenderer {

    public static void renderPanel(
            DrawContext ctx,
            int mx,
            int my,
            int panelX,
            int panelY,
            net.minecraft.client.font.TextRenderer textRenderer,
            List<Module> modules,
            int listScroll,
            Module selectedModule,
            int settingsScroll
    ) {
        ctx.fill(panelX + 3, panelY + 3, panelX + PANEL_W + 3, panelY + PANEL_H + 3, COLOR_SHADOW);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, COLOR_PANEL_BG);
        drawBorder(ctx, panelX, panelY, PANEL_W, PANEL_H, COLOR_PANEL_BORDER);

        int listRight = panelX + LIST_W;
        ctx.fill(panelX, panelY, listRight, panelY + PANEL_H, COLOR_LIST_BG);
        ctx.fill(listRight, panelY, listRight + DIVIDER_W, panelY + PANEL_H, COLOR_DIVIDER);

        renderModuleList(ctx, mx, my, panelX, panelY, textRenderer, modules, listScroll, selectedModule);
        renderSettingsPanel(ctx, panelX, panelY, textRenderer, selectedModule, settingsScroll);
        renderLangLabel(ctx, panelX, panelY, textRenderer);
    }

    private static void renderModuleList(
            DrawContext ctx,
            int mx,
            int my,
            int panelX,
            int panelY,
            net.minecraft.client.font.TextRenderer textRenderer,
            List<Module> modules,
            int listScroll,
            Module selectedModule
    ) {
        int listRight = panelX + LIST_W;
        int clipTop = panelY + PAD;
        int clipBottom = panelY + PANEL_H - PAD;
        int visibleCount = (PANEL_H - PAD * 2) / ENTRY_H;
        int maxScroll = Math.max(0, modules.size() - visibleCount);
        int startIndex = Math.clamp(listScroll, 0, maxScroll);

        ctx.enableScissor(panelX, clipTop, listRight, clipBottom);

        for (int i = startIndex; i < modules.size(); i++) {
            int relativeIndex = i - startIndex;
            int entryY = panelY + PAD + relativeIndex * ENTRY_H;

            if (entryY + ENTRY_H > clipBottom) {
                break;
            }

            Module module = modules.get(i);
            boolean isSelected = module == selectedModule;
            boolean isHovered = mx >= panelX && mx < listRight && my >= entryY && my < entryY + ENTRY_H;
            boolean isActive = module.isActive();

            int bg = isSelected ? COLOR_ENTRY_SELECTED
                : isHovered ? COLOR_ENTRY_HOVER
                : COLOR_ENTRY_NORMAL;

            ctx.fill(panelX, entryY, listRight, entryY + ENTRY_H, bg);

            int dotColor = isActive ? COLOR_ACTIVE_DOT : COLOR_INACTIVE_DOT;
            ctx.fill(panelX + 4, entryY + ENTRY_H / 2 - 2, panelX + 7, entryY + ENTRY_H / 2 + 2, dotColor);

            int nameColor = isActive ? COLOR_TEXT_ACTIVE : COLOR_TEXT_PRIMARY;
            ctx.drawTextWithShadow(
                textRenderer,
                Text.literal(module.getName()),
                panelX + 12,
                entryY + (ENTRY_H - 8) / 2,
                nameColor
            );
        }

        ctx.disableScissor();
    }

    private static void renderSettingsPanel(
            DrawContext ctx,
            int panelX,
            int panelY,
            net.minecraft.client.font.TextRenderer textRenderer,
            Module selectedModule,
            int settingsScroll
    ) {
        if (selectedModule == null) {
            ctx.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Select a module"),
                panelX + LIST_W + DIVIDER_W + (PANEL_W - LIST_W - DIVIDER_W) / 2,
                panelY + PANEL_H / 2,
                COLOR_TEXT_DIM
            );

            return;
        }

        int settingsX = panelX + LIST_W + DIVIDER_W + PAD;
        int settingsRight = panelX + PANEL_W - PAD;
        int settingsW = PANEL_W - LIST_W - DIVIDER_W - PAD * 2;
        int contentTop = panelY + PAD + ROW_H + ROW_GAP;
        int panelBottom = panelY + PANEL_H - PAD;

        ctx.drawTextWithShadow(
            textRenderer,
            Text.literal(selectedModule.getName()),
            settingsX,
            panelY + PAD + (ROW_H - 8) / 2,
            COLOR_TITLE
        );

        ctx.enableScissor(settingsX, contentTop, settingsRight, panelBottom);

        int y = contentTop - settingsScroll;

        for (SettingGroup group : selectedModule.getSettings().getGroups()) {
            if (y + ROW_H > contentTop && y < panelBottom) {
                ctx.drawTextWithShadow(
                    textRenderer,
                    Text.literal(group.name()),
                    settingsX,
                    y + (ROW_H - 8) / 2,
                    COLOR_GROUP_HEADER
                );
                ctx.fill(settingsX, y + ROW_H, settingsX + settingsW, y + ROW_H + 1, COLOR_GROUP_LINE);
            }

            y += ROW_H + ROW_GAP;

            for (Setting<?> setting : group.settings()) {
                if (setting.isVisible() == false) {
                    continue;
                }

                if (y + SETTING_H > contentTop && y < panelBottom) {
                    ctx.drawTextWithShadow(
                        textRenderer,
                        Text.literal(setting.getName()),
                        settingsX,
                        y + (LABEL_H - 8) / 2,
                        COLOR_TEXT_DIM
                    );
                }

                y += SETTING_H + ROW_GAP;
            }
        }

        ctx.disableScissor();
    }

    private static void renderLangLabel(
            DrawContext ctx,
            int panelX,
            int panelY,
            net.minecraft.client.font.TextRenderer textRenderer
    ) {
        String langLabel = "Lang: ";
        int langLabelX = panelX + PANEL_W - LANG_FIELD_W - LANG_BTN_W - PAD
            - textRenderer.getWidth(langLabel) - LANG_BTN_GAP;
        int langLabelY = panelY + PANEL_H + PAD + (ROW_H - 8) / 2;
        ctx.drawTextWithShadow(textRenderer, Text.literal(langLabel), langLabelX, langLabelY, COLOR_TEXT_DIM);
    }
}
