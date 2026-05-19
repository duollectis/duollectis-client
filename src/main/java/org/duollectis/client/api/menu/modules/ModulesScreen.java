package org.duollectis.client.api.menu.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.duollectis.client.api.i18n.ClientLocale;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;
import org.duollectis.client.api.menu.widget.StyledSliderWidget;
import org.duollectis.client.api.menu.widget.StyledToggleButton;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleManager;

import java.util.ArrayList;
import java.util.List;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

public class ModulesScreen extends Screen {

    final List<Module> modules = new ArrayList<>(ModuleManager.getInstance().all());

    int listScroll = 0;
    int settingsScroll = 0;
    float settingsScrollSmooth = 0f;
    float settingsScrollTarget = 0f;
    Module selectedModule = null;

    private boolean draggingPanel = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    final List<EnumDropdownWidget<?>> dropdowns = new ArrayList<>();
    final List<EnumDropdownWidget<ClientLocale>> langDropdowns = new ArrayList<>();
    final List<StyledSliderWidget> sliders = new ArrayList<>();
    final List<StyledToggleButton> toggles = new ArrayList<>();
    final List<TextFieldWidget> textFields = new ArrayList<>();

    EnumDropdownWidget<ClientLocale> langDropdown;
    int panelX;
    int panelY;

    public ModulesScreen() {
        super(Text.literal("Modules"));
    }

    public static void open() {
        MinecraftClient.getInstance().setScreen(new ModulesScreen());
    }

    TextRenderer textRenderer() {
        return textRenderer;
    }

    <T extends Element & Drawable & Selectable> T register(T widget) {
        return addDrawableChild(widget);
    }

    void rebuildSettingsWidgets() {
        clearChildren();
        dropdowns.clear();
        langDropdowns.clear();
        sliders.clear();
        toggles.clear();
        textFields.clear();

        ModulesScreenInit.langDropdown(this);
        ModulesScreenInit.settingsWidgets(this);
    }

    void selectModule(Module module) {
        selectedModule = module;
        settingsScroll = 0;
        settingsScrollSmooth = 0f;
        settingsScrollTarget = 0f;
        rebuildSettingsWidgets();
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        ModulesScreenInit.langDropdown(this);
        ModulesScreenInit.settingsWidgets(this);
    }

    @Override
    public void removed() {
        ModuleManager.getInstance().save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        interpolateScroll(delta);
        renderBackground(ctx, mx, my, delta);
        ModulesScreenRenderer.renderPanel(
            ctx, mx, my, panelX, panelY, textRenderer, modules, listScroll, selectedModule, settingsScroll
        );

        int contentTop = panelY + PAD + ROW_H + ROW_GAP;
        int panelBottom = panelY + PANEL_H - PAD;
        int settingsLeft = panelX + LIST_W + DIVIDER_W;
        int settingsRight = panelX + PANEL_W;

        int renderMx = draggingPanel ? -1 : mx;
        int renderMy = draggingPanel ? -1 : my;

        ctx.enableScissor(settingsLeft, contentTop, settingsRight, panelBottom);
        renderSettingsWidgets(ctx, renderMx, renderMy, delta);
        ctx.disableScissor();

        if (langDropdown != null) {
            langDropdown.render(ctx, mx, my, delta);
        }

        for (EnumDropdownWidget<?> dropdown : dropdowns) {
            dropdown.renderOverlay(ctx, mx, my);
        }

        for (EnumDropdownWidget<ClientLocale> dropdown : langDropdowns) {
            dropdown.renderOverlay(ctx, mx, my);
        }
    }

    private void renderSettingsWidgets(DrawContext ctx, int mx, int my, float delta) {
        for (StyledToggleButton toggle : toggles) {
            toggle.render(ctx, mx, my, delta);
        }

        for (StyledSliderWidget slider : sliders) {
            slider.render(ctx, mx, my, delta);
        }

        for (EnumDropdownWidget<?> dropdown : dropdowns) {
            dropdown.render(ctx, mx, my, delta);
        }

        for (TextFieldWidget field : textFields) {
            field.render(ctx, mx, my, delta);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, COLOR_OVERLAY_BG);
    }

    @Override
    public boolean mouseClicked(Click event, boolean handled) {
        if (draggingPanel) {
            return true;
        }

        double mx = event.x();
        double my = event.y();
        int btn = event.button();
        int listRight = panelX + LIST_W;
        int panelRight = panelX + PANEL_W;
        int contentTop = panelY + PAD + ROW_H + ROW_GAP;
        int panelBottom = panelY + PANEL_H - PAD;

        boolean inSettingsPanel = mx >= listRight && mx < panelRight && my >= panelY && my < panelY + PANEL_H;

        if (inSettingsPanel && isScrolling()) {
            return true;
        }

        for (EnumDropdownWidget<ClientLocale> dropdown : langDropdowns) {
            if (dropdown.handleClick(mx, my)) {
                return true;
            }
        }

        boolean inSettingsClip = mx >= listRight && mx < panelRight && my >= contentTop && my < panelBottom;

        if (inSettingsClip) {
            for (EnumDropdownWidget<?> dropdown : dropdowns) {
                if (dropdown.handleClick(mx, my)) {
                    return true;
                }
            }

            for (StyledSliderWidget slider : sliders) {
                if (slider.handleClick(mx, my)) {
                    return true;
                }
            }

            for (StyledToggleButton toggle : toggles) {
                if (toggle.handleClick(mx, my)) {
                    return true;
                }
            }
        }

        if (mx >= panelX && mx < listRight && my >= panelY && my < panelY + PANEL_H) {
            if (ModulesScreenActions.handleModuleListClick(this, mx, my, btn)) {
                return true;
            }
        }

        if (btn == 0 && mx >= panelX && mx < panelX + PANEL_W && my >= panelY && my < panelY + PANEL_H) {
            draggingPanel = true;
            dragOffsetX = mx - panelX;
            dragOffsetY = my - panelY;
            return true;
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(Click event, double offsetX, double offsetY) {
        if (event.button() != 0) {
            return super.mouseDragged(event, offsetX, offsetY);
        }

        if (draggingPanel) {
            panelX = (int) Math.clamp(event.x() - dragOffsetX, 0, width - PANEL_W);
            panelY = (int) Math.clamp(event.y() - dragOffsetY, 0, height - PANEL_H);
            rebuildSettingsWidgets();
            return true;
        }

        for (StyledSliderWidget slider : sliders) {
            if (slider.handleDrag(event.x())) {
                return true;
            }
        }

        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click event) {
        if (event.button() == 0) {
            draggingPanel = false;

            for (StyledSliderWidget slider : sliders) {
                slider.handleRelease();
            }
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int listRight = panelX + LIST_W;
        int panelRight = panelX + PANEL_W;

        if (mx >= panelX && mx < listRight && my >= panelY && my < panelY + PANEL_H) {
            int visibleCount = (PANEL_H - PAD * 2) / ENTRY_H;
            listScroll = Math.clamp(
                (int) (listScroll - vAmt),
                0,
                Math.max(0, modules.size() - visibleCount)
            );
            return true;
        }

        if (mx >= listRight && mx < panelRight && my >= panelY && my < panelY + PANEL_H) {
            int step = ROW_H + ROW_GAP;
            int maxScroll = computeMaxSettingsScroll();
            settingsScrollTarget = Math.clamp(settingsScrollTarget + (float) (-vAmt * step), 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    boolean isScrolling() {
        return Math.abs(settingsScrollTarget - settingsScrollSmooth) > 0.5f;
    }

    private void interpolateScroll(float delta) {
        float speed = 1f - (float) Math.pow(0.01, delta);
        settingsScrollSmooth += (settingsScrollTarget - settingsScrollSmooth) * speed;

        int newScroll = Math.round(settingsScrollSmooth);

        if (newScroll == settingsScroll) {
            return;
        }

        settingsScroll = newScroll;
        rebuildSettingsWidgets();
    }

    private int computeMaxSettingsScroll() {
        if (selectedModule == null) {
            return 0;
        }

        int groupStep = ROW_H + ROW_GAP;
        int settingStep = SETTING_H + ROW_GAP;
        int visibleH = PANEL_H - PAD * 2 - ROW_H - ROW_GAP;
        int contentH = 0;

        for (var group : selectedModule.getSettings().getGroups()) {
            contentH += groupStep;

            for (var setting : group.settings()) {
                if (setting.isVisible() == false) {
                    continue;
                }

                contentH += settingStep;
            }
        }

        return Math.max(0, contentH - visibleH);
    }

}
