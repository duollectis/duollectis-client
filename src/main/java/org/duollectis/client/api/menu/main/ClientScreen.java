package org.duollectis.client.api.menu.main;

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
import org.duollectis.client.api.menu.accounts.AccountsPanelActions;
import org.duollectis.client.api.menu.accounts.AccountsPanelInit;
import org.duollectis.client.api.menu.accounts.AccountsPanelRenderer;
import org.duollectis.client.api.menu.modules.ModulesPanelInit;
import org.duollectis.client.api.menu.modules.ModulesPanelRenderer;
import org.duollectis.client.api.menu.widget.*;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleManager;

import java.util.*;

import static org.duollectis.client.api.menu.main.ClientScreenLayout.*;
import static org.duollectis.client.utils.MC.mc;

/**
 * Главный экран управления клиентом. Отображает список модулей слева
 * и панель настроек выбранного модуля справа. Поддерживает перетаскивание панели,
 * плавную прокрутку настроек и выпадающий список выбора языка.
 * Содержит встроенную вкладку управления аккаунтами.
 * <p>
 * Логика ввода делегируется в {@link ClientScreenInput},
 * логика прокрутки — в {@link ClientScreenScroll},
 * рендеринг модулей — в {@link ModulesPanelRenderer},
 * рендеринг аккаунтов — в {@link AccountsPanelRenderer},
 * инициализация виджетов модулей — в {@link ModulesPanelInit},
 * инициализация виджетов аккаунтов — в {@link AccountsPanelInit},
 * обработка действий с аккаунтами — в {@link AccountsPanelActions}.
 */
public class ClientScreen extends Screen {

	public enum Tab {
		MODULES,
		ACCOUNTS
	}

	public final List<Module> modules = new ArrayList<>(ModuleManager.getInstance().all());

	public int listScroll;
	public int settingsScroll;
	public float settingsScrollSmooth;
	public float settingsScrollTarget;
	public Module selectedModule;

	public boolean draggingPanel;
	public double dragOffsetX;
	public double dragOffsetY;

	public final List<EnumDropdownWidget<?>> dropdowns = new ArrayList<>();
	public final List<EnumDropdownWidget<ClientLocale>> langDropdowns = new ArrayList<>();
	public final List<StyledSliderWidget> sliders = new ArrayList<>();
	public final List<StyledToggleButton> toggles = new ArrayList<>();
	public final List<BoolToggleWidget> boolToggles = new ArrayList<>();
	public final List<TextFieldWidget> textFields = new ArrayList<>();
	public final List<ItemListWidget> itemListWidgets = new ArrayList<>();

	public EnumDropdownWidget<ClientLocale> langDropdown;
	public int panelX;
	public int panelY;

	public Tab activeTab = Tab.MODULES;

	public int accListScroll;
	public Account accSelected;
	public String accStatusMsg = "";
	public int accStatusColor = 0xFFAAAAAA;
	public List<Account> accCached = Collections.emptyList();
	public ClientScreenLayout.AccSortMode accSortMode = ClientScreenLayout.AccSortMode.GROUP;

	public String accRenamingGroup = null;
	public TextFieldWidget accGroupRenameField = null;

	public Account accRenamingAccount = null;
	public TextFieldWidget accAccountRenameField = null;

	public int[] accAddAccountBtn = new int[4];
	public int[] accAddGroupBtn = new int[4];

	public final Map<String, Boolean> collapsedGroups = new HashMap<>();
	public final Map<String, Float> groupArrowAngles = new HashMap<>();

	public ConfirmDialog confirmDialog = null;

	public Module bindListeningModule = null;

	public final AccountsPanelActions.AccStatusSink accStatus = (msg, color) -> {
		accStatusMsg = msg;
		accStatusColor = color;
	};

	private static final ClientScreen instance = new ClientScreen();

	private ClientScreen() {
		super(Text.literal("Client"));
	}

	public static void open() {
		mc.setScreen(instance);
	}

	public TextRenderer textRenderer() {
		return textRenderer;
	}

	public <T extends Element & Drawable & Selectable> T register(T widget) {
		return addDrawableChild(widget);
	}

	public void rebuildSettingsWidgets() {
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

		ModulesPanelInit.langDropdown(this);

		if (activeTab == Tab.MODULES) {
			ModulesPanelInit.settingsWidgets(this);
		}
		else {
			AccountsPanelInit.accountWidgets(this);
		}
	}

	public void selectModule(Module module) {
		selectedModule = module;
		settingsScroll = 0;
		settingsScrollSmooth = 0f;
		settingsScrollTarget = 0f;
		activeTab = Tab.MODULES;
		rebuildSettingsWidgets();
	}

	public void switchTab(Tab tab) {
		activeTab = tab;
		rebuildSettingsWidgets();
	}

	public boolean isScrolling() {
		return ClientScreenScroll.isScrolling(this);
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
			ClientScreenScroll.interpolate(this, delta);
		}

		renderBackground(ctx, mx, my, delta);

		ModulesPanelRenderer.renderPanel(
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
		}
		else {
			List<Account> raw = AccountManager.getInstance().getAccounts();
			accCached = AccountsPanelActions.sortAccounts(raw, accSortMode);
			AccountsPanelRenderer.renderAccountsPanel(this, ctx, mx, my);

			for (TextFieldWidget field : textFields) {
				field.render(ctx, mx, my, delta);
			}

			if (confirmDialog != null) {
					confirmDialog.render(ctx, textRenderer, mx, my, width, height);
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
		return ClientScreenInput.mouseClicked(this, event, handled) || super.mouseClicked(event, handled);
	}

	@Override
	public boolean mouseDragged(Click event, double offsetX, double offsetY) {
		boolean consumed = ClientScreenInput.mouseDragged(this, event);
		return consumed || super.mouseDragged(event, offsetX, offsetY);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return ClientScreenInput.keyPressed(this, input) || super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		return ClientScreenInput.charTyped(this, input) || super.charTyped(input);
	}

	@Override
	public boolean mouseReleased(Click event) {
		ClientScreenInput.mouseReleased(this, event);
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
		return ClientScreenInput.mouseScrolled(this, mx, my, vAmt) || super.mouseScrolled(mx, my, hAmt, vAmt);
	}
}
