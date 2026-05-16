package com.example.client;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.add(new Module("Fast Break") {
            @Override
            public void onTick() {
                // Logic
            }
        });
        modules.add(new Module("Fullbright") {
            @Override
            public void onTick() {
                // Logic
            }
        });
    }

    public static void onClientTick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }
}
