package org.duollectis.client.api.menu.modules;

import org.duollectis.client.api.modules.Module;

import java.util.List;

import static org.duollectis.client.api.menu.modules.ModulesScreenLayout.*;

final class ModulesScreenActions {

    private ModulesScreenActions() {}

    static boolean handleModuleListClick(ModulesScreen screen, double mx, double my, int btn) {
        List<Module> modules = screen.modules;
        int visibleCount = (PANEL_H - PAD * 2) / ENTRY_H;
        int startIndex = Math.clamp(screen.listScroll, 0, Math.max(0, modules.size() - visibleCount));

        for (int i = startIndex; i < modules.size(); i++) {
            int entryY = screen.panelY + PAD + (i - startIndex) * ENTRY_H;

            if (entryY + ENTRY_H > screen.panelY + PANEL_H - PAD) {
                break;
            }

            if (my < entryY || my >= entryY + ENTRY_H) {
                continue;
            }

            if (btn == 0) {
                screen.selectModule(modules.get(i));
            } else if (btn == 1) {
                modules.get(i).toggle();
            }

            return true;
        }

        return false;
    }
}
