package org.duollectis.client.api.clickgui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.duollectis.client.api.clickgui.overlay.ClickGuiOverlay;
import org.duollectis.client.api.clickgui.widget.ClickGuiSettingWidget;
import org.duollectis.client.api.clickgui.widget.ClickGuiWidgetFactory;
import org.duollectis.client.api.modules.Category;
import org.duollectis.client.api.modules.Module;
import org.duollectis.client.api.modules.ModuleManager;
import org.duollectis.client.api.settings.Setting;
import org.duollectis.client.utils.JsonHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.duollectis.client.utils.MC.gameDir;
import static org.duollectis.client.utils.MC.mc;

/**
 * ClickGui — второй экран управления клиентом в стиле классических клиентов.
 * Отображает колонки категорий с модулями. ЛКМ по модулю — включить/выключить.
 * ПКМ по модулю — развернуть/свернуть настройки прямо под ним.
 * ЛКМ по заголовку категории — перетаскивание панели (позиция сохраняется).
 * ПКМ по заголовку категории — свернуть/развернуть панель.
 * <p>
 * Поддерживает систему оверлеев: любой виджет может открыть {@link ClickGuiOverlay}
 * через {@link #openOverlay}, который будет рендериться поверх всего и перехватывать ввод.
 */
@Slf4j
public class ClickGuiScreen extends Screen {

	private static final String LAYOUT_FILE = "duollectis/clickgui-layout.json";

	/** Состояние одной колонки категории: позиция, ширина и флаг свёрнутости. */
	public static final class CategoryPanel {

		public final Category category;
		public int x;
		public int y;
		/** Актуальная ширина панели, вычисляется рендерером по длине имён модулей. */
		public int width = ClickGuiLayout.PANEL_W;
		public boolean collapsed;
		public boolean dragging;
		public double dragOffsetX;
		public double dragOffsetY;

		public CategoryPanel(Category category, int x, int y) {
			this.category = category;
			this.x = x;
			this.y = y;
		}
	}

	private static final ClickGuiScreen INSTANCE = new ClickGuiScreen();

	/** Модули, у которых развёрнута панель настроек (ПКМ). */
	public final Set<Module> expandedModules = new LinkedHashSet<>();

	/** Скролл настроек для каждого развёрнутого модуля. */
	public final Map<Module, Integer> settingsScroll = new HashMap<>();

	/** Панели категорий в порядке отображения. */
	public final List<CategoryPanel> panels = new ArrayList<>();

	/**
	 * Виджеты настроек: для каждого развёрнутого модуля хранится
	 * упорядоченный список виджетов в том же порядке, что и видимые настройки.
	 */
	public final Map<Module, List<ClickGuiSettingWidget>> moduleWidgets = new LinkedHashMap<>();

	/** Модуль, ожидающий назначения клавиши. */
	public Module bindListeningModule = null;

	/** Активный оверлей (null — нет активного). Рендерится поверх всего, перехватывает ввод. */
	public ClickGuiOverlay activeOverlay = null;

	/**
	 * Флаг: позиции панелей ещё не были выровнены по реальным ширинам.
	 * Устанавливается при первом открытии (до загрузки сохранённого лейаута),
	 * чтобы рендерер расставил панели корректно.
	 */
	public boolean panelPositionsDirty = true;

	private ClickGuiScreen() {
		super(Text.literal("ClickGUI"));
		initPanels();
	}

	public static void open() {
		mc.setScreen(INSTANCE);
	}

	public static ClickGuiScreen getInstance() {
		return INSTANCE;
	}

	public TextRenderer textRenderer() {
		return textRenderer;
	}

	@Override
	protected void init() {
		loadLayout();
		rebuildWidgets();
	}

	@Override
	public void removed() {
		activeOverlay = null;
		saveLayout();
		ModuleManager.getInstance().save();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Пересоздаёт виджеты для всех развёрнутых модулей. */
	public void rebuildWidgets() {
		clearChildren();
		moduleWidgets.clear();

		for (Module module : expandedModules) {
			buildWidgetsFor(module);
		}
	}

	/** Разворачивает или сворачивает панель настроек модуля. */
	public void toggleExpanded(Module module) {
		if (expandedModules.contains(module)) {
			expandedModules.remove(module);
			settingsScroll.remove(module);
			moduleWidgets.remove(module);
		}
		else {
			expandedModules.add(module);
			settingsScroll.put(module, 0);
			buildWidgetsFor(module);
		}
	}

	/** Возвращает виджеты для модуля (только если он развёрнут). */
	public List<ClickGuiSettingWidget> widgetsFor(Module module) {
		return moduleWidgets.getOrDefault(module, List.of());
	}

	/**
	 * Открывает оверлей поверх ClickGui.
	 * Инициализирует его по текущим размерам экрана.
	 */
	public void openOverlay(ClickGuiOverlay overlay) {
		activeOverlay = overlay;
		activeOverlay.init(textRenderer, width, height);
	}

	/** Закрывает активный оверлей. */
	public void closeOverlay() {
		activeOverlay = null;
	}

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		renderBackground(ctx, mx, my, delta);
		ClickGuiRenderer.renderAll(this, ctx, mx, my, textRenderer);

		if (activeOverlay != null) {
			activeOverlay.render(ctx, textRenderer, mx, my);
		}
	}

	@Override
	public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
		ctx.fill(0, 0, width, height, 0x88050810);
	}

	@Override
	public boolean mouseClicked(Click event, boolean handled) {
		return ClickGuiInput.mouseClicked(this, event) || super.mouseClicked(event, handled);
	}

	@Override
	public boolean mouseDragged(Click event, double offsetX, double offsetY) {
		return ClickGuiInput.mouseDragged(this, event) || super.mouseDragged(event, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click event) {
		ClickGuiInput.mouseReleased(this, event);
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
		return ClickGuiInput.mouseScrolled(this, mx, my, vAmt) || super.mouseScrolled(mx, my, hAmt, vAmt);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return ClickGuiInput.keyPressed(this, input) || super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		return ClickGuiInput.charTyped(this, input) || super.charTyped(input);
	}

	/** Возвращает список модулей для данной категории. */
	public List<Module> modulesFor(Category category) {
		return ModuleManager.getInstance()
			.all()
			.stream()
			.filter(m -> {
				var info = m.getClass().getAnnotation(org.duollectis.client.api.modules.ModuleInfo.class);
				return info != null && info.category() == category;
			})
			.toList();
	}

	private void buildWidgetsFor(Module module) {
		List<Setting<?>> visible = module.getSettings().all()
			.stream()
			.filter(Setting::isVisible)
			.toList();

		List<ClickGuiSettingWidget> widgets = new ArrayList<>(visible.size());

		for (Setting<?> setting : visible) {
			widgets.add(ClickGuiWidgetFactory.create(setting));
		}

		moduleWidgets.put(module, widgets);
	}

	/**
	 * Сохраняет позиции и состояние свёрнутости всех панелей в {@code duollectis/clickgui-layout.json}.
	 * Вызывается при закрытии экрана.
	 */
	private void saveLayout() {
		File file = resolveLayoutFile();
		file.getParentFile().mkdirs();

		JsonObject root = new JsonObject();

		for (CategoryPanel panel : panels) {
			JsonObject entry = new JsonObject();
			entry.addProperty("x", panel.x);
			entry.addProperty("y", panel.y);
			entry.addProperty("collapsed", panel.collapsed);
			root.add(panel.category.name(), entry);
		}

		try (FileWriter writer = new FileWriter(file)) {
			JsonHelper.GSON.toJson(root, writer);
		}
		catch (IOException e) {
			log.error("Не удалось сохранить лейаут ClickGui", e);
		}
	}

	/**
	 * Загружает позиции панелей из {@code duollectis/clickgui-layout.json}.
	 * Если файл отсутствует — устанавливает флаг {@code panelPositionsDirty},
	 * чтобы рендерер расставил панели автоматически по реальным ширинам.
	 */
	private void loadLayout() {
		File file = resolveLayoutFile();

		if (!file.exists()) {
			panelPositionsDirty = true;
			return;
		}

		try (FileReader reader = new FileReader(file)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			for (CategoryPanel panel : panels) {
				String key = panel.category.name();

				if (!root.has(key)) {
					continue;
				}

				JsonObject entry = root.getAsJsonObject(key);
				panel.x = entry.get("x").getAsInt();
				panel.y = entry.get("y").getAsInt();
				panel.collapsed = entry.get("collapsed").getAsBoolean();
			}

			panelPositionsDirty = false;
		}
		catch (Exception e) {
			log.error("Не удалось загрузить лейаут ClickGui", e);
			panelPositionsDirty = true;
		}
	}

	private void initPanels() {
		panels.clear();

		int startX = 10;
		int startY = 10;
		int gap = ClickGuiLayout.PANEL_W + ClickGuiLayout.PANEL_GAP;
		int col = 0;

		for (Category category : Category.values()) {
			if (category == Category.NONE) {
				continue;
			}

			panels.add(new CategoryPanel(category, startX + col * gap, startY));
			col++;
		}

		panels.add(new CategoryPanel(Category.NONE, startX + col * gap, startY));
	}

	private static File resolveLayoutFile() {
		return gameDir().resolve(LAYOUT_FILE).toFile();
	}
}
