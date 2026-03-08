package com.directwriting.ime;

/**
 * Normalized key action generated from key view IDs/tags.
 */
public final class KeyAction {

    public enum Type {
        CHARACTER,
        SHIFT,
        BACKSPACE,
        SPACE,
        ENTER,
        COMMA,
        PERIOD,
        LAYOUT_TOGGLE,
        LANGUAGE_TOGGLE,
        MODE_TOGGLE,
        QUICKBAR_CLOSE,
        NUMBER_ROW_TOGGLE,
        CLIPBOARD_PASTE,
        SEND_HANDWRITING,
        CLEAR_HANDWRITING,
        UNDO_HANDWRITING,
        REDO_HANDWRITING,
        PEN_TOOL,
        ERASER_TOOL,
        NONE
    }

    private final Type type;
    private final String value;

    private KeyAction(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public static KeyAction character(String keyCode) {
        return new KeyAction(Type.CHARACTER, keyCode);
    }

    public static KeyAction of(Type type) {
        return new KeyAction(type, null);
    }

    public static KeyAction none() {
        return of(Type.NONE);
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
