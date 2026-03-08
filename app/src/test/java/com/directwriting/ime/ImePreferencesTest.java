package com.directwriting.ime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImePreferencesTest {

    @Test
    public void clampPenThickness_keepsValueInRange() {
        assertEquals(ImePreferences.MIN_PEN_THICKNESS, ImePreferences.clampPenThickness(0));
        assertEquals(10, ImePreferences.clampPenThickness(10));
        assertEquals(ImePreferences.MAX_PEN_THICKNESS, ImePreferences.clampPenThickness(99));
    }

    @Test
    public void clampEraserSize_keepsValueInRange() {
        assertEquals(ImePreferences.MIN_ERASER_SIZE, ImePreferences.clampEraserSize(1));
        assertEquals(24, ImePreferences.clampEraserSize(24));
        assertEquals(ImePreferences.MAX_ERASER_SIZE, ImePreferences.clampEraserSize(300));
    }

    @Test
    public void normalizeEraserMode_unknownFallsBackToStroke() {
        assertEquals(ImePreferences.ERASER_MODE_STROKE, ImePreferences.normalizeEraserMode(-1));
        assertEquals(ImePreferences.ERASER_MODE_STROKE, ImePreferences.normalizeEraserMode(42));
        assertEquals(ImePreferences.ERASER_MODE_AREA, ImePreferences.normalizeEraserMode(ImePreferences.ERASER_MODE_AREA));
    }
}
