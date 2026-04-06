package com.erodev.sodiumrelief.compat.modmenu;

import net.minecraft.client.gui.screen.Screen;

public final class ModMenuCompat {
    private ModMenuCompat() {
    }

    public static Screen createConfigScreen(Screen parent) {
        return ReliefConfigScreenFactory.create(parent);
    }
}
