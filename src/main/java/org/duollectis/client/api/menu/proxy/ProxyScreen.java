package org.duollectis.client.api.menu.proxy;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.duollectis.client.api.proxy.Proxy;
import org.duollectis.client.api.proxy.ProxyChecker;
import org.duollectis.client.api.proxy.ProxyManager;
import org.duollectis.client.api.menu.widget.EnumDropdownWidget;

import java.util.*;

import static org.duollectis.client.api.menu.proxy.ProxyScreenUtil.*;
import static org.duollectis.client.api.menu.proxy.ProxyScreenLayout.*;

public class ProxyScreen extends Screen {

    private static final long DOUBLE_CLICK_MS = 400;

    static int barTotal = -1;
    static int barBaseline = 0;
    static int barDone = -1;

    int listScroll = 0;
    boolean addressSplitting = false;
    int lastClickIdx = -1;
    long lastClickTime = 0;

    Proxy editingProxy = null;

    TextFieldWidget nameField;
    TextFieldWidget addressField;
    TextFieldWidget portField;
    TextFieldWidget usernameField;
    TextFieldWidget passwordField;

    TextFieldWidget maxPingsField;
    TextFieldWidget maxDelayField;
    TextFieldWidget parallelismField;
    TextFieldWidget targetField;
    TextFieldWidget targetPortField;
    ButtonWidget btnPingToggle;

    EnumDropdownWidget<Proxy.Type> typeDropdown;
    EnumDropdownWidget<ProxyFilter> filterDropdown;
    EnumDropdownWidget<SortMode> sortDropdown;

    ButtonWidget btnStartStop;
    ButtonWidget btnDelFailed;
    ButtonWidget btnClear;
    ButtonWidget btnResetRaw;
    ButtonWidget btnExportOk;
    ButtonWidget btnResetChk;
    ButtonWidget btnNext;

    ProxyFilter savedFilter = null;
    SortMode savedSort = null;

    List<Proxy> cachedList = Collections.emptyList();
    List<ProxyListEntry> cachedEntries = Collections.emptyList();
    final Set<Proxy> expandedProxies = new HashSet<>();

    String statusMsg = "";
    int statusColor = 0xFFAAAAAA;

    final ProxyScreenActions.StatusSink statusSink = (msg, color) -> {
        statusMsg = msg;
        statusColor = color;
    };

    public ProxyScreen() {
        super(Text.translatable("screen.duollectis.proxy_manager.title"));
        ProxyChecker.getInstance().loadSettings();

        ProxyChecker.Settings settings = ProxyChecker.getInstance().getSettings();

        try {
            savedFilter = ProxyFilter.valueOf(settings.getFilterMode());
        } catch (Exception ignored) {
            savedFilter = ProxyFilter.ALL;
        }

        try {
            savedSort = SortMode.valueOf(settings.getSortMode());
        } catch (Exception ignored) {
            savedSort = SortMode.NONE;
        }
    }

    int lx() {
        return listX();
    }

    int ly() {
        return listY();
    }

    int lw() {
        return listW(width);
    }

    int lh() {
        return listH(height);
    }

    int mv() {
        return maxVis(height);
    }

    int sx() {
        return sidebarX(width);
    }

    int sw() {
        return sidebarW();
    }

    TextRenderer textRenderer() {
        return textRenderer;
    }

    <T extends Element & Drawable & Selectable> T register(T widget) {
        return addDrawableChild(widget);
    }

    @Override
    protected void init() {
        ProxyScreenInit.filterDropdown(this);
        ProxyScreenInit.formFields(this);
        ProxyScreenInit.checkerSettings(this);
        ProxyScreenInit.buttons(this);
    }

    void saveUiSettings() {
        ProxyChecker.Settings settings = ProxyChecker.getInstance().getSettings();

        if (filterDropdown != null) {
            settings.setFilterMode(filterDropdown.getSelected().name());
        }

        if (sortDropdown != null) {
            settings.setSortMode(sortDropdown.getSelected().name());
        }

        ProxyChecker.getInstance().saveSettings();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xC0101010);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, PAD, 0xFFFFFF);

        cachedList = ProxyManager.getInstance().getProxies();
        cachedList = filterDropdown.getSelected().apply(cachedList);
        cachedList = sortDropdown.getSelected().apply(cachedList);
        cachedEntries = buildEntries(cachedList);

        ProxyScreenRenderer.renderList(this, ctx, cachedEntries, mx, my);
        ProxyScreenRenderer.renderSidebarLabels(this, ctx);
        ProxyScreenRenderer.renderOverlay(this, ctx, cachedList);
        ProxyScreenRenderer.updateButtonStates(this, cachedList);

        super.render(ctx, mx, my, delta);

        if (filterDropdown != null) {
            filterDropdown.renderOverlay(ctx, mx, my);
        }

        if (typeDropdown != null) {
            typeDropdown.renderOverlay(ctx, mx, my);
        }

        if (sortDropdown != null) {
            sortDropdown.renderOverlay(ctx, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(Click event, boolean handled) {
        double mx = event.x(), my = event.y();
        int btn = event.button();

        if (btn == 0 || btn == 1) {
            if (filterDropdown != null && filterDropdown.handleClick(mx, my)) {
                return true;
            }

            if (typeDropdown != null && typeDropdown.handleClick(mx, my)) {
                return true;
            }

            if (sortDropdown != null && sortDropdown.handleClick(mx, my)) {
                return true;
            }
        }

        int lx = lx(), ly = ly(), lw = lw(), lh = lh();

        if (btn == 0 && mx >= lx && mx < lx + lw && my >= ly && my < ly + lh) {
            int idx = (int) (my - ly) / ENTRY_H + listScroll;

            if (idx >= 0 && idx < cachedEntries.size()) {
                ProxyListEntry entry = cachedEntries.get(idx);
                Proxy clicked = entry.parent();

                if (!entry.isChild()) {
                    String copyLabel = translatableString("screen.duollectis.proxy_manager.entry.copy_btn");
                    int copyW = textRenderer.getWidth(copyLabel) + 4;
                    int copyX = lx + lw - copyW - 2;
                    int entryY = ly + (idx - listScroll) * ENTRY_H;

                    if (mx >= copyX && mx < copyX + copyW && my >= entryY && my < entryY + ENTRY_H) {
                        String toCopy = clicked.getAddress() + ":" + clicked.getPort();
                        client.keyboard.setClipboard(toCopy);
                        statusMsg = tsa("screen.duollectis.proxy_manager.msg.copied", toCopy);
                        statusColor = 0xFF88FFAA;
                        setFocused(null);
                        return true;
                    }
                }

                if (entry.isDualParent()) {
                    if (expandedProxies.contains(clicked)) {
                        expandedProxies.remove(clicked);
                    } else {
                        expandedProxies.add(clicked);
                    }

                    setFocused(null);
                    return true;
                }

                if (entry.isChild()) {
                    ProxyManager pm = ProxyManager.getInstance();
                    Proxy.Type childType = entry.childType();

                    if (pm.getActiveProxy() == clicked && clicked.getType() == childType) {
                        pm.setActiveProxy(null);
                    } else {
                        clicked.setType(childType);
                        pm.setActiveProxy(clicked);
                    }

                    setFocused(null);
                    return true;
                }

                long now = System.currentTimeMillis();
                boolean isDoubleClick = (idx == lastClickIdx) && (now - lastClickTime <= DOUBLE_CLICK_MS);
                lastClickIdx = idx;
                lastClickTime = now;

                if (isDoubleClick) {
                    startEdit(clicked);
                } else if (clicked.getStatus() == Proxy.Status.OK) {
                    ProxyManager pm = ProxyManager.getInstance();
                    pm.setActiveProxy(pm.getActiveProxy() == clicked ? null : clicked);
                }

                setFocused(null);
                return true;
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.isEnter()) {
            boolean focusedOnCheckerField = (maxPingsField != null && maxPingsField.isFocused())
                    || (maxDelayField != null && maxDelayField.isFocused())
                    || (parallelismField != null && parallelismField.isFocused());

            if (focusedOnCheckerField) {
                applyCheckerSettings();
                statusMsg = translatableString("screen.duollectis.proxy_manager.msg.settings_applied");
                statusColor = 0xFF88FF88;
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        int lx = lx(), ly = ly(), lw = lw(), lh = lh();
        int delta = -(int) Math.signum(vAmt);

        if (mouseX >= lx && mouseX < lx + lw && mouseY >= ly && mouseY < ly + lh) {
            listScroll = Math.clamp(listScroll + delta, 0, cachedEntries.size() - mv());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    void startEdit(Proxy proxy) {
        editingProxy = proxy;
        clearAndInit();
        statusMsg = translatableString("screen.duollectis.proxy_manager.msg.editing");
        statusColor = 0xFF88CCFF;
    }

    void saveEdit() {
        ProxyScreenActions.saveEdit(
                editingProxy,
                addressField.getText().trim(),
                portField.getText().trim(),
                nameField.getText().trim(),
                usernameField.getText().trim(),
                passwordField.getText().trim(),
                typeDropdown != null ? typeDropdown.getSelected() : Proxy.Type.SOCKS5,
                statusSink,
                () -> {
                    editingProxy = null;
                    clearAndInit();
                }
        );
    }

    void cancelEdit() {
        editingProxy = null;
        statusMsg = "";
        clearAndInit();
    }

    void addProxy() {
        ProxyScreenActions.addProxy(
                addressField.getText().trim(),
                portField.getText().trim(),
                nameField.getText().trim(),
                usernameField.getText().trim(),
                passwordField.getText().trim(),
                typeDropdown != null ? typeDropdown.getSelected() : Proxy.Type.SOCKS5
        );
        nameField.setText("");
        addressField.setText("");
        portField.setText("");
        usernameField.setText("");
        passwordField.setText("");
    }

    void removeSelected() {
        ProxyScreenActions.removeProxy(
                editingProxy,
                () -> {
                    editingProxy = null;
                    clearAndInit();
                }
        );
    }

    void toggleStartStop() {
        ProxyScreenActions.toggleStartStop(
                this::applyCheckerSettings,
                (total, baseline) -> {
                    barTotal = total;
                    barBaseline = baseline;
                    barDone = -1;
                },
                frozen -> barDone = frozen
        );
    }

    void deleteFailed() {
        ProxyScreenActions.deleteFailed(statusSink);
    }

    void clearRaw() {
        ProxyScreenActions.clearRaw(
                editingProxy,
                statusSink,
                () -> {
                    editingProxy = null;
                    listScroll = 0;
                    clearAndInit();
                }
        );
        listScroll = 0;
    }

    void resetRaw() {
        ProxyScreenActions.resetRaw(statusSink);
    }

    void resetChecked() {
        ProxyScreenActions.resetChecked(statusSink);
        listScroll = 0;
    }

    void importClipboard() {
        ProxyScreenActions.importClipboard(client, statusSink);
    }

    void importFile() {
        ProxyScreenActions.importFile(statusSink);
    }

    void exportOk() {
        ProxyScreenActions.exportOk(statusSink);
    }

    void nextProxy() {
        List<Proxy> sorted = sortDropdown != null
                ? sortDropdown.getSelected().apply(ProxyManager.getInstance().getProxies())
                : ProxyManager.getInstance().getProxies();

        List<ProxySlot> slots = buildOkSlots(sorted);

        if (slots.isEmpty()) {
            statusMsg = translatableString("screen.duollectis.proxy_manager.msg.no_ok_proxy");
            statusColor = 0xFFFFAA44;
            return;
        }

        ProxyManager pm = ProxyManager.getInstance();
        Proxy current = pm.getActiveProxy();
        Proxy.Type currentType = current != null ? current.getType() : null;
        int currentIdx = findSlotIndex(slots, current, currentType);

        int nextIdx = (currentIdx + 1) % slots.size();
        ProxySlot next = slots.get(nextIdx);
        next.proxy().setType(next.type());
        pm.setActiveProxy(next.proxy());

        statusMsg = tsa(
                "screen.duollectis.proxy_manager.msg.next_proxy",
                next.proxy().getAddress() + ":" + next.proxy().getPort() + " [" + next.type() + "]"
        );

        statusColor = 0xFF88FFAA;
    }

    void applyCheckerSettings() {
        ProxyScreenActions.applyCheckerSettings(
                maxPingsField != null ? maxPingsField.getText() : "",
                maxDelayField != null ? maxDelayField.getText() : "",
                parallelismField != null ? parallelismField.getText() : "",
                targetField != null ? targetField.getText() : "",
                targetPortField != null ? targetPortField.getText() : ""
        );
    }

    private List<ProxyListEntry> buildEntries(List<Proxy> proxies) {
        List<ProxyListEntry> result = new ArrayList<>();

        for (Proxy p : proxies) {
            if (p.isDualType() && p.getStatus() == Proxy.Status.OK) {
                result.add(ProxyListEntry.dualParent(p));

                if (expandedProxies.contains(p)) {
                    if (p.getSupportedTypes().contains(Proxy.Type.SOCKS4)) {
                        result.add(ProxyListEntry.child(p, Proxy.Type.SOCKS4));
                    }

                    if (p.getSupportedTypes().contains(Proxy.Type.SOCKS5)) {
                        result.add(ProxyListEntry.child(p, Proxy.Type.SOCKS5));
                    }
                }
            } else {
                result.add(ProxyListEntry.single(p));
            }
        }

        return result;
    }

    private static List<ProxySlot> buildOkSlots(List<Proxy> sorted) {
        List<ProxySlot> slots = new ArrayList<>();

        for (Proxy p : sorted) {
            if (p.getStatus() != Proxy.Status.OK) {
                continue;
            }

            if (p.isDualType()) {
                if (p.getSupportedTypes().contains(Proxy.Type.SOCKS4)) {
                    slots.add(new ProxySlot(p, Proxy.Type.SOCKS4));
                }

                if (p.getSupportedTypes().contains(Proxy.Type.SOCKS5)) {
                    slots.add(new ProxySlot(p, Proxy.Type.SOCKS5));
                }
            } else {
                slots.add(new ProxySlot(p, p.getType()));
            }
        }

        return slots;
    }

    private static int findSlotIndex(List<ProxySlot> slots, Proxy current, Proxy.Type currentType) {
        for (int i = 0; i < slots.size(); i++) {
            ProxySlot slot = slots.get(i);

            if (slot.proxy() == current && slot.type() == currentType) {
                return i;
            }
        }

        return -1;
    }

    private record ProxySlot(Proxy proxy, Proxy.Type type) {}
}
