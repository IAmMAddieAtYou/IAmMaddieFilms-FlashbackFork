package com.moulberry.flashback.exporting.depthsettings;

import com.moulberry.flashback.combo_options.ComboOption;

public enum DEPTHVISUALS implements ComboOption {

    LEVELS("Show World Only"),
    ENTITIES("Show World and Entities"),
    PARTICLES("Show World, Entities and Particles.");

    private final String text;

    DEPTHVISUALS(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return this.text;
    }
}