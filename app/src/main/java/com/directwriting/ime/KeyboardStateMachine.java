package com.directwriting.ime;

/**
 * Stateful keyboard state machine used by the IME.
 */
public class KeyboardStateMachine {

    static final long CAPS_LOCK_DOUBLE_TAP_WINDOW_MS = 400L;

    private KeyboardState.Mode mode = KeyboardState.Mode.KEYBOARD;
    private KeyboardState.Language language = KeyboardState.Language.KO;
    private KeyboardState.Layout layout = KeyboardState.Layout.ALPHA;
    private KeyboardState.ShiftState shiftState = KeyboardState.ShiftState.OFF;
    private boolean quickBarVisible = true;
    private boolean numberRowVisible = true;
    private long lastShiftTapTimestamp = 0L;

    public KeyboardState snapshot() {
        return new KeyboardState(mode, language, layout, shiftState, quickBarVisible, numberRowVisible);
    }

    public KeyboardState.Mode getMode() {
        return mode;
    }

    public KeyboardState.Language getLanguage() {
        return language;
    }

    public KeyboardState.Layout getLayout() {
        return layout;
    }

    public KeyboardState.ShiftState getShiftState() {
        return shiftState;
    }

    public boolean isQuickBarVisible() {
        return quickBarVisible;
    }

    public boolean isNumberRowVisible() {
        return numberRowVisible;
    }

    public boolean isShifted() {
        return shiftState != KeyboardState.ShiftState.OFF;
    }

    public void resetForNewInput() {
        mode = KeyboardState.Mode.KEYBOARD;
        layout = KeyboardState.Layout.ALPHA;
        shiftState = KeyboardState.ShiftState.OFF;
        quickBarVisible = true;
        numberRowVisible = true;
        lastShiftTapTimestamp = 0L;
    }

    public void toggleMode() {
        mode = (mode == KeyboardState.Mode.KEYBOARD)
                ? KeyboardState.Mode.HANDWRITING
                : KeyboardState.Mode.KEYBOARD;
    }

    public void toggleLanguage() {
        language = (language == KeyboardState.Language.KO)
                ? KeyboardState.Language.EN
                : KeyboardState.Language.KO;
        layout = KeyboardState.Layout.ALPHA;
        shiftState = KeyboardState.ShiftState.OFF;
    }

    public void toggleLayout() {
        layout = (layout == KeyboardState.Layout.ALPHA)
                ? KeyboardState.Layout.SYMBOL
                : KeyboardState.Layout.ALPHA;
        shiftState = KeyboardState.ShiftState.OFF;
        lastShiftTapTimestamp = 0L;
    }

    public void closeQuickBar() {
        quickBarVisible = false;
    }

    public void toggleNumberRow() {
        numberRowVisible = !numberRowVisible;
    }

    public void onShiftPressed(long nowMillis) {
        if (layout == KeyboardState.Layout.SYMBOL) {
            shiftState = (shiftState == KeyboardState.ShiftState.OFF)
                    ? KeyboardState.ShiftState.ON
                    : KeyboardState.ShiftState.OFF;
            return;
        }

        if (shiftState == KeyboardState.ShiftState.CAPS_LOCK) {
            shiftState = KeyboardState.ShiftState.OFF;
            lastShiftTapTimestamp = 0L;
            return;
        }

        if (shiftState == KeyboardState.ShiftState.OFF) {
            shiftState = KeyboardState.ShiftState.ON;
            lastShiftTapTimestamp = nowMillis;
            return;
        }

        if (nowMillis - lastShiftTapTimestamp <= CAPS_LOCK_DOUBLE_TAP_WINDOW_MS) {
            shiftState = KeyboardState.ShiftState.CAPS_LOCK;
        } else {
            shiftState = KeyboardState.ShiftState.OFF;
        }
        lastShiftTapTimestamp = nowMillis;
    }

    public void consumeSingleShift() {
        if (layout == KeyboardState.Layout.ALPHA && shiftState == KeyboardState.ShiftState.ON) {
            shiftState = KeyboardState.ShiftState.OFF;
        }
    }
}
