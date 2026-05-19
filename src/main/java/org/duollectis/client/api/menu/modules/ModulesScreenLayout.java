package org.duollectis.client.api.menu.modules;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ModulesScreenLayout {

    public static final int PANEL_W = 520;
    public static final int PANEL_H = 340;
    public static final int LIST_W = 150;
    public static final int DIVIDER_W = 1;
    public static final int PAD = 8;
    public static final int ENTRY_H = 22;
    public static final int ROW_H = 20;
    public static final int ROW_GAP = 5;
    public static final int LABEL_H = 10;
    public static final int SETTING_H = LABEL_H + ROW_H;
    public static final int SLIDER_INPUT_W = 44;
    public static final int LANG_FIELD_W = 80;
    public static final int LANG_BTN_W = 50;
    public static final int LANG_BTN_GAP = 4;

    public static final int COLOR_PANEL_BG = 0xF0111318;
    public static final int COLOR_PANEL_BORDER = 0xFF2A3A5A;
    public static final int COLOR_LIST_BG = 0xFF0E1016;
    public static final int COLOR_DIVIDER = 0xFF1E2A40;
    public static final int COLOR_ENTRY_NORMAL = 0xFF0E1016;
    public static final int COLOR_ENTRY_HOVER = 0xFF161C28;
    public static final int COLOR_ENTRY_SELECTED = 0xFF1A2A48;
    public static final int COLOR_ACTIVE_DOT = 0xFF55EE55;
    public static final int COLOR_INACTIVE_DOT = 0xFF444444;
    public static final int COLOR_TEXT_PRIMARY = 0xFFDDDDDD;
    public static final int COLOR_TEXT_DIM = 0xFF778899;
    public static final int COLOR_TEXT_ACTIVE = 0xFF66FF88;
    public static final int COLOR_GROUP_HEADER = 0xFF4488BB;
    public static final int COLOR_GROUP_LINE = 0xFF1E3050;
    public static final int COLOR_TITLE = 0xFFCCDDFF;
    public static final int COLOR_BTN_BG = 0xFF1A2030;
    public static final int COLOR_BTN_HOVER = 0xFF223050;
    public static final int COLOR_BTN_ACTIVE = 0xFF1A4020;
    public static final int COLOR_BTN_ACTIVE_HOV = 0xFF225030;
    public static final int COLOR_BTN_BORDER = 0xFF2A3A5A;
    public static final int COLOR_BTN_TEXT = 0xFFCCCCCC;
    public static final int COLOR_BTN_TEXT_ON = 0xFF66FF88;
    public static final int COLOR_OVERLAY_BG = 0x88050810;
    public static final int COLOR_SHADOW = 0x55000000;

    public static void drawBorder(net.minecraft.client.gui.DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
