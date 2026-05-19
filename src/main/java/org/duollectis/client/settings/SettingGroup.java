package org.duollectis.client.settings;

import org.duollectis.client.api.settings.Setting;

import java.util.List;

public record SettingGroup(String name, List<Setting<?>> settings) {}
