package org.duollectis.client.api.menu.modules;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.duollectis.client.api.account.Account;
import org.duollectis.client.api.account.AccountManager;
import org.duollectis.client.api.i18n.ClientLocale;
import org.duollectis.client.api.menu.widget.BoolToggleWidget;
import org.duollectis.client.api.menu.widget.ConfirmDialog;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;
import org.duollectis.client.api.menu.widget.ItemListWidget;
import org.duollectis.client.api.menu.widget.StyledSliderWidget;
import org.duollectis.client.api.menu.widget.StyledToggleButton;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.api.settings.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;
import static org.duollectis.client.utils.MC.mc;

/**
 * Главный экран управления модулями клиента. Отображает список модулей слева
 * и панель настроек выбранного модуля справа. Поддерживает перетаскивание панели,
 * плавную прокрутку настроек и выпадающий список выбора языка.
 * Содержит встроенную вкладку управления аккаунтами.
 */
public class ModulesScreen extends Screen {

	enum Tab { MODULES, ACCOUNTS }

	final List<Module> modules = new ArrayList<>(ModuleManager.getInstance().all());

	int listScroll;
	int settingsScroll;
	float settingsScrollSmooth;
	float settingsScrollTarget;
	Module selectedModule;

	private boolean draggingPanel;
	private double dragOffsetX;
	private double dragOffsetY;

	final List<EnumDropdownWidget<?>> dropdowns = new ArrayList<>();
	final List<EnumDropdownWidget<ClientLocale>> langDropdowns = new ArrayList<>();
	final List<StyledSliderWidget> sliders = new ArrayList<>();
	final List<StyledToggleButton> toggles = new ArrayList<>();
	final List<BoolToggleWidget> boolToggles = new ArrayList<>();
	final List<TextFieldWidget> textFields = new ArrayList<>();
	final List<ItemListWidget> itemListWidgets = new ArrayList<>();

	EnumDropdownWidget<ClientLocale> langDropdown;
	int panelX;
	int panelY;

	Tab activeTab = Tab.MODULES;

	int accListScroll;
	Account accSelected;
	String accStatusMsg = "";
	int accStatusColor = 0xFFAAAAAA;
	List<Account> accCached = Collections.emptyList();
	ModulesScreenLayout.AccSortMode accSortMode = ModulesScreenLayout.AccSortMode.GROUP;
	/** Имя группы, которая сейчас переименовывается (null = режим переименования неактивен). */
	String accRenamingGroup = null;
	TextFieldWidget accGroupRenameField = null;

	/** Аккаунт, который сейчас переименовывается (null = режим переименования неактивен). */
	Account accRenamingAccount = null;
	TextFieldWidget accAccountRenameField = null;

	/** Координаты кнопок панели аккаунтов: {x, y, w, h}. */
	int[] accAddAccountBtn = new int[4];
	int[] accAddGroupBtn = new int[4];
	int[] accCollapseAllBtn = new int[4];
	int[] accExpandAllBtn = new int[4];

	/** Свёрнутые группы: имя группы → true если свёрнута. */
	final Map<String, Boolean> collapsedGroups = new HashMap<>();

	/** Текущий угол стрелки группы (0.0 = свёрнуто, 1.0 = развёрнуто). */
	final Map<String, Float> groupArrowAngles = new HashMap<>();

	/** Активный диалог подтверждения (null = диалог не показывается). */
	ConfirmDialog confirmDialog = null;

	/** Модуль, ожидающий назначения клавиши (null = режим прослушивания неактивен). */
	Module bindListeningModule = null;

	final ModulesScreenActions.AccStatusSink accStatus = (msg, color) -> {
		accStatusMsg = msg;
		accStatusColor = color;
	};

	private static ModulesScreen instance;

	public ModulesScreen() {
		super(Text.literal("Modules"));
	}

	public static void open() {
		if (instance == null) {
			instance = new ModulesScreen();
		}

		mc.setScreen(instance);
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
		boolToggles.clear();
		textFields.clear();
		itemListWidgets.clear();
		accGroupRenameField = null;
		accAccountRenameField = null;

		ModulesScreenInit.langDropdown(this);

		if (activeTab == Tab.MODULES) {
			ModulesScreenInit.settingsWidgets(this);
		} else {
			ModulesScreenInit.accountWidgets(this);
		}
	}

	void selectModule(Module module) {
		selectedModule = module;
		settingsScroll = 0;
		settingsScrollSmooth = 0f;
		settingsScrollTarget = 0f;
		rebuildSettingsWidgets();
	}

	void switchTab(Tab tab) {
		activeTab = tab;
		rebuildSettingsWidgets();
	}

	@Override
	protected void init() {
		panelX = (width - PANEL_W) / 2;
		panelY = (height - PANEL_H) / 2;

		rebuildSettingsWidgets();
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
		if (activeTab == Tab.MODULES) {
			interpolateScroll(delta);
		}

		renderBackground(ctx, mx, my, delta);

		ModulesScreenRenderer.renderPanel(
				ctx,
				mx,
				my,
				panelX,
				panelY,
				textRenderer,
				modules,
				listScroll,
				selectedModule,
				settingsScroll,
				activeTab,
				bindListeningModule
		);

		if (activeTab == Tab.MODULES) {
			accCached = Collections.emptyList();

			int contentTop = panelY + PAD + ROW_H;
			int panelBottom = panelY + PANEL_H - PAD;
			int settingsLeft = panelX + LIST_W + DIVIDER_W;
			int settingsRight = panelX + PANEL_W;

			int renderMx = draggingPanel ? -1 : mx;
			int renderMy = draggingPanel ? -1 : my;

			ctx.enableScissor(settingsLeft, contentTop, settingsRight, panelBottom);
			renderSettingsWidgets(ctx, renderMx, renderMy, delta);
			ctx.disableScissor();
		} else {
			List<Account> raw = AccountManager.getInstance().getAccounts();
			accCached = ModulesScreenActions.sortAccounts(raw, accSortMode);
			ModulesScreenRenderer.renderAccountsPanel(this, ctx, mx, my);

			for (TextFieldWidget field : textFields) {
				field.render(ctx, mx, my, delta);
			}

			if (confirmDialog != null) {
				ModulesScreenRenderer.renderConfirmDialog(ctx, textRenderer, confirmDialog, mx, my, width, height);
			}
		}

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
		for (BoolToggleWidget toggle : boolToggles) {
			toggle.render(ctx, mx, my, delta);
		}

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

		for (ItemListWidget widget : itemListWidgets) {
			widget.render(ctx, mx, my, delta);
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

		if (bindListeningModule != null && btn == 0) {
			bindListeningModule = null;
			return true;
		}

		if (activeTab == Tab.ACCOUNTS && confirmDialog != null && btn == 0) {
			boolean consumed = ModulesScreenRenderer.handleConfirmDialogClick(confirmDialog, mx, my, width, height);
			confirmDialog = null;
			return consumed;
		}

		if (ModulesScreenActions.handleTabClick(this, mx, my)) {
			return true;
		}

		if (activeTab == Tab.ACCOUNTS) {
			if (ModulesScreenActions.handleAccountsPanelClick(this, mx, my, btn)) {
				return true;
			}
		}

		int listRight = panelX + LIST_W;
		int panelRight = panelX + PANEL_W;
		int contentTop = panelY + PAD + ROW_H;
		int panelBottom = panelY + PANEL_H - PAD;

		boolean inListPanel = mx >= panelX
			&& mx < listRight
			&& my >= panelY
			&& my < panelY + PANEL_H;

		boolean inSettingsPanel = mx >= listRight
			&& mx < panelRight
			&& my >= panelY
			&& my < panelY + PANEL_H;

		if (inSettingsPanel && isScrolling()) {
			return true;
		}

		for (EnumDropdownWidget<ClientLocale> dropdown : langDropdowns) {
			if (dropdown.handleClick(mx, my)) {
				return true;
			}
		}

		boolean inSettingsClip = mx >= listRight
			&& mx < panelRight
			&& my >= contentTop
			&& my < panelBottom;

		if (inSettingsClip) {
			boolean hitField = textFields.stream().anyMatch(f -> f.isMouseOver(mx, my));

			if (hitField) {
				for (TextFieldWidget field : textFields) {
					if (field.isMouseOver(mx, my)) {
						setFocused(field);
						return true;
					}
				}
			} else {
				setFocused(null);
			}

			for (ItemListWidget widget : itemListWidgets) {
				if (widget.mouseClicked(event, handled)) {
					return true;
				}
			}

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

			for (BoolToggleWidget toggle : boolToggles) {
				if (toggle.handleClick(mx, my)) {
					return true;
				}
			}

			for (StyledToggleButton toggle : toggles) {
				if (toggle.handleClick(mx, my)) {
					return true;
				}
			}
		} else {
			setFocused(null);
		}

		if (inListPanel && btn == 1 && ModulesScreenActions.handleBindButtonClick(this, mx, my)) {
			return true;
		}

		if (inListPanel && ModulesScreenActions.handleModuleListClick(this, mx, my, btn)) {
			return true;
		}

		if (btn == 0
			&& mx >= panelX
			&& mx < panelX + PANEL_W
			&& my >= panelY
			&& my < panelY + PANEL_H
		) {
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
	public boolean keyPressed(KeyInput input) {
		if (bindListeningModule != null) {
			if (input.isEscape()) {
				bindListeningModule.clearBind();
			} else {
				bindListeningModule.setKeyCode(input.key());
			}

			bindListeningModule = null;
			return true;
		}

		if (activeTab == Tab.ACCOUNTS && accGroupRenameField != null && accGroupRenameField.isFocused()) {
			if (input.isEnter()) {
				ModulesScreenActions.commitGroupRename(this);
				return true;
			}

			if (input.isEscape()) {
				ModulesScreenActions.cancelGroupRename(this);
				return true;
			}

			accGroupRenameField.keyPressed(input);
			return true;
		}

		if (activeTab == Tab.ACCOUNTS && accAccountRenameField != null && accAccountRenameField.isFocused()) {
			if (input.isEnter()) {
				ModulesScreenActions.commitAccountRename(this);
				return true;
			}

			if (input.isEscape()) {
				ModulesScreenActions.cancelAccountRename(this);
				return true;
			}

			accAccountRenameField.keyPressed(input);
			return true;
		}

		for (TextFieldWidget field : textFields) {
			if (field.isFocused() && field.keyPressed(input)) {
				return true;
			}
		}

		for (ItemListWidget widget : itemListWidgets) {
			if (widget.isFocused() && widget.keyPressed(input)) {
				return true;
			}
		}

		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (activeTab == Tab.ACCOUNTS && accGroupRenameField != null && accGroupRenameField.isFocused()) {
			accGroupRenameField.charTyped(input);
			return true;
		}

		if (activeTab == Tab.ACCOUNTS && accAccountRenameField != null && accAccountRenameField.isFocused()) {
			accAccountRenameField.charTyped(input);
			return true;
		}

		for (TextFieldWidget field : textFields) {
			if (field.isFocused() && field.charTyped(input)) {
				return true;
			}
		}

		for (ItemListWidget widget : itemListWidgets) {
			if (widget.isFocused() && widget.charTyped(input)) {
				return true;
			}
		}

		return super.charTyped(input);
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
		if (activeTab == Tab.ACCOUNTS) {
			return ModulesScreenActions.handleAccountsScroll(this, mx, my, vAmt);
		}

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
			int step = textRenderer.fontHeight + NAME_GAP + ROW_H;
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

		int dividerH = SETTING_DIVIDER_PAD * 2 + 1;
		int visibleH = PANEL_H - PAD * 2 - ROW_H;
		int contentH = 0;

		for (var group : selectedModule.getSettings().getGroups()) {
			contentH += ROW_H;

			var visibleSettings = group.settings()
				.stream()
				.filter(Setting::isVisible)
				.toList();

			for (int i = 0; i < visibleSettings.size(); i++) {
				contentH += ModulesScreenRenderer.itemHeight(textRenderer, visibleSettings.get(i));

				if (i < visibleSettings.size() - 1) {
					contentH += dividerH;
				}
			}
		}

		return Math.max(0, contentH - visibleH);
	}

}
