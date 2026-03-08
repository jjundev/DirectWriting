package com.directwriting.ime;

import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps keyboard view IDs/tags into normalized key actions.
 */
public final class KeyActionMapper {

    private static final Map<Integer, KeyAction> ID_ACTIONS = new HashMap<>();

    static {
        ID_ACTIONS.put(R.id.key_shift, KeyAction.of(KeyAction.Type.SHIFT));
        ID_ACTIONS.put(R.id.key_backspace, KeyAction.of(KeyAction.Type.BACKSPACE));
        ID_ACTIONS.put(R.id.key_space, KeyAction.of(KeyAction.Type.SPACE));
        ID_ACTIONS.put(R.id.key_space_right, KeyAction.of(KeyAction.Type.SPACE));
        ID_ACTIONS.put(R.id.key_enter, KeyAction.of(KeyAction.Type.ENTER));
        ID_ACTIONS.put(R.id.key_comma, KeyAction.of(KeyAction.Type.COMMA));
        ID_ACTIONS.put(R.id.key_period, KeyAction.of(KeyAction.Type.PERIOD));
        ID_ACTIONS.put(R.id.key_layout_toggle, KeyAction.of(KeyAction.Type.LAYOUT_TOGGLE));
        ID_ACTIONS.put(R.id.key_language_toggle, KeyAction.of(KeyAction.Type.LANGUAGE_TOGGLE));
        ID_ACTIONS.put(R.id.btn_mode_toggle, KeyAction.of(KeyAction.Type.MODE_TOGGLE));
        ID_ACTIONS.put(R.id.btn_mode_toggle_handwriting, KeyAction.of(KeyAction.Type.MODE_TOGGLE));
        ID_ACTIONS.put(R.id.key_quickbar_close, KeyAction.of(KeyAction.Type.QUICKBAR_CLOSE));
        ID_ACTIONS.put(R.id.key_number_row_toggle, KeyAction.of(KeyAction.Type.NUMBER_ROW_TOGGLE));
        ID_ACTIONS.put(R.id.key_clipboard_chip, KeyAction.of(KeyAction.Type.CLIPBOARD_PASTE));
        ID_ACTIONS.put(R.id.btn_send, KeyAction.of(KeyAction.Type.SEND_HANDWRITING));
        ID_ACTIONS.put(R.id.btn_clear, KeyAction.of(KeyAction.Type.CLEAR_HANDWRITING));
        ID_ACTIONS.put(R.id.btn_undo, KeyAction.of(KeyAction.Type.UNDO_HANDWRITING));
        ID_ACTIONS.put(R.id.btn_redo, KeyAction.of(KeyAction.Type.REDO_HANDWRITING));
        ID_ACTIONS.put(R.id.btn_pen_tool, KeyAction.of(KeyAction.Type.PEN_TOOL));
        ID_ACTIONS.put(R.id.btn_eraser_tool, KeyAction.of(KeyAction.Type.ERASER_TOOL));
    }

    private KeyActionMapper() {
    }

    public static KeyAction map(View view) {
        Object tag = view.getTag();
        if (tag instanceof String) {
            return KeyAction.character((String) tag);
        }
        return mapById(view.getId());
    }

    static KeyAction mapById(int viewId) {
        KeyAction action = ID_ACTIONS.get(viewId);
        return action == null ? KeyAction.none() : action;
    }
}
