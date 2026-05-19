package org.duollectis.client.api.menu;

/**
 * Делегат для обратной совместимости.
 * Реальная реализация находится в {@link org.duollectis.client.api.menu.modules.ModulesScreen}.
 *
 * @deprecated Используй {@link org.duollectis.client.api.menu.modules.ModulesScreen} напрямую.
 */
@Deprecated
public final class ModulesScreen {

    private ModulesScreen() {}

    public static void open() {
        org.duollectis.client.api.menu.modules.ModulesScreen.open();
    }
}
