package com.directwriting.ime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class KeyboardStateMachineTest {

    @Test
    public void defaultState_isKeyboardKoAlphaShiftOff() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        assertEquals(KeyboardState.Mode.KEYBOARD, machine.getMode());
        assertEquals(KeyboardState.Language.KO, machine.getLanguage());
        assertEquals(KeyboardState.Layout.ALPHA, machine.getLayout());
        assertEquals(KeyboardState.ShiftState.OFF, machine.getShiftState());
        assertTrue(machine.isQuickBarVisible());
        assertTrue(machine.isNumberRowVisible());
    }

    @Test
    public void toggleMode_switchesKeyboardAndHandwriting() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        machine.toggleMode();
        assertEquals(KeyboardState.Mode.HANDWRITING, machine.getMode());

        machine.toggleMode();
        assertEquals(KeyboardState.Mode.KEYBOARD, machine.getMode());
    }

    @Test
    public void toggleLanguage_resetsLayoutAndShift() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        machine.toggleLayout();
        machine.onShiftPressed(100L);
        machine.toggleLanguage();

        assertEquals(KeyboardState.Language.EN, machine.getLanguage());
        assertEquals(KeyboardState.Layout.ALPHA, machine.getLayout());
        assertEquals(KeyboardState.ShiftState.OFF, machine.getShiftState());
    }

    @Test
    public void shiftDoubleTap_enablesCapsLock() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        machine.onShiftPressed(100L);
        assertEquals(KeyboardState.ShiftState.ON, machine.getShiftState());

        machine.onShiftPressed(300L);
        assertEquals(KeyboardState.ShiftState.CAPS_LOCK, machine.getShiftState());

        machine.onShiftPressed(900L);
        assertEquals(KeyboardState.ShiftState.OFF, machine.getShiftState());
    }

    @Test
    public void consumeSingleShift_turnsOneshotShiftOff() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        machine.onShiftPressed(100L);
        machine.consumeSingleShift();

        assertEquals(KeyboardState.ShiftState.OFF, machine.getShiftState());
    }

    @Test
    public void shiftInSymbolLayout_togglesSymbolPageState() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        machine.toggleLayout();
        assertEquals(KeyboardState.Layout.SYMBOL, machine.getLayout());

        machine.onShiftPressed(100L);
        assertEquals(KeyboardState.ShiftState.ON, machine.getShiftState());

        machine.onShiftPressed(200L);
        assertEquals(KeyboardState.ShiftState.OFF, machine.getShiftState());
    }

    @Test
    public void resetForNewInput_setsKeyboardAlphaShiftOff() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        machine.toggleMode();
        machine.toggleLayout();
        machine.onShiftPressed(100L);
        machine.closeQuickBar();
        machine.toggleNumberRow();
        machine.resetForNewInput();

        assertEquals(KeyboardState.Mode.KEYBOARD, machine.getMode());
        assertEquals(KeyboardState.Layout.ALPHA, machine.getLayout());
        assertEquals(KeyboardState.ShiftState.OFF, machine.getShiftState());
        assertTrue(machine.isQuickBarVisible());
        assertTrue(machine.isNumberRowVisible());
    }

    @Test
    public void quickBarAndNumberRow_toggleIndependently() {
        KeyboardStateMachine machine = new KeyboardStateMachine();

        machine.closeQuickBar();
        assertFalse(machine.isQuickBarVisible());
        assertTrue(machine.isNumberRowVisible());

        machine.toggleNumberRow();
        assertFalse(machine.isQuickBarVisible());
        assertFalse(machine.isNumberRowVisible());

        machine.toggleNumberRow();
        assertTrue(machine.isNumberRowVisible());
    }
}
