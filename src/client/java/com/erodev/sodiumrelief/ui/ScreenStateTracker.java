package com.erodev.sodiumrelief.ui;

public final class ScreenStateTracker {
    private String lastScreenClass = "";
    private String lastLanguage = "";

    public String lastScreenClass() {
        return lastScreenClass;
    }

    public void lastScreenClass(String value) {
        this.lastScreenClass = value;
    }

    public String lastLanguage() {
        return lastLanguage;
    }

    public void lastLanguage(String value) {
        this.lastLanguage = value;
    }
}
