package com.moulberry.flashback.exporting.depthsettings;

import com.moulberry.flashback.combo_options.ComboOption;

public enum DEPTHEXPORT implements ComboOption {

    HIGHPRECISION("32bit"),
    NORMALPRECISION("16bit");

    private final String text;

    DEPTHEXPORT(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return this.text;
    }
}