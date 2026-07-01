package com.erodev.sodiumrelief.compat.modmenu;

import net.minecraft.client.gui.screens.Screen;

public final class ModMenuCompat {
    private ModMenuCompat() {
    }

    public static Screen createConfigScreen(Screen parent) {
        return ReliefConfigScreenFactory.create(parent);
    }
}
