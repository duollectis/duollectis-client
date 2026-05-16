package com.example.client;

public abstract class Module {
    private final String name;
    private boolean enabled;

    public Module(String name) {
        this.name = name;
        this.enabled = ModSettings.toggles.getOrDefault(name, false);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        ModSettings.toggles.put(name, enabled);
    }

    public boolean isEnabled() {
        return ModSettings.toggles.getOrDefault(name, false);
    }

    public String getName() {
        return name;
    }

    public abstract void onTick();
}
