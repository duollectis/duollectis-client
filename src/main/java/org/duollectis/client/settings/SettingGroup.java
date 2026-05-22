package org.duollectis.client.settings;

import org.duollectis.client.api.settings.Setting;

import java.util.List;

/** Именованная группа настроек для визуальной группировки в UI панели модуля. */
public record SettingGroup(String id, List<Setting<?>> settings) {}
