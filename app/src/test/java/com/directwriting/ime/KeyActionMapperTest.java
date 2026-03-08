package com.directwriting.ime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeyActionMapperTest {

    @Test
    public void mapById_keySpaceRight_mapsToSpaceAction() {
        KeyAction action = KeyActionMapper.mapById(R.id.key_space_right);
        assertEquals(KeyAction.Type.SPACE, action.getType());
    }

    @Test
    public void mapById_btnPenTool_mapsToPenToolAction() {
        KeyAction action = KeyActionMapper.mapById(R.id.btn_pen_tool);
        assertEquals(KeyAction.Type.PEN_TOOL, action.getType());
    }

    @Test
    public void mapById_btnEraserTool_mapsToEraserToolAction() {
        KeyAction action = KeyActionMapper.mapById(R.id.btn_eraser_tool);
        assertEquals(KeyAction.Type.ERASER_TOOL, action.getType());
    }

    @Test
    public void mapById_btnRedo_mapsToRedoHandwritingAction() {
        KeyAction action = KeyActionMapper.mapById(R.id.btn_redo);
        assertEquals(KeyAction.Type.REDO_HANDWRITING, action.getType());
    }
}
