package com.directwriting.ime;

/**
 * Immutable keyboard state snapshot.
 */
public final class KeyboardState {

    public enum Mode {
        KEYBOARD,
        HANDWRITING
    }

    public enum Language {
        KO,
        EN
    }

    public enum Layout {
        ALPHA,
        SYMBOL
    }

    public enum ShiftState {
        OFF,
        ON,
        CAPS_LOCK
    }

    private final Mode mode;
    private final Language language;
    private final Layout layout;
    private final ShiftState shiftState;
    private final boolean quickBarVisible;
    private final boolean numberRowVisible;

    public KeyboardState(
            Mode mode,
            Language language,
            Layout layout,
            ShiftState shiftState,
            boolean quickBarVisible,
            boolean numberRowVisible
    ) {
        this.mode = mode;
        this.language = language;
        this.layout = layout;
        this.shiftState = shiftState;
        this.quickBarVisible = quickBarVisible;
        this.numberRowVisible = numberRowVisible;
    }

    public Mode getMode() {
        return mode;
    }

    public Language getLanguage() {
        return language;
    }

    public Layout getLayout() {
        return layout;
    }

    public ShiftState getShiftState() {
        return shiftState;
    }

    public boolean isQuickBarVisible() {
        return quickBarVisible;
    }

    public boolean isNumberRowVisible() {
        return numberRowVisible;
    }
}
