package com.directwriting.ime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeyboardHeightCalculatorTest {

    private final KeyboardHeightCalculator.BaseHeights baseHeights =
            new KeyboardHeightCalculator.BaseHeights(38, 46, 50, 50, 50, 56);

    @Test
    public void calculate_quickBarAndNumberRowVisible_usesPortraitRatioAndKeepsTargetTotal() {
        KeyboardHeightCalculator.Result result = KeyboardHeightCalculator.calculate(
                2400,
                true,
                0.42f,
                0.55f,
                300,
                4300,
                12,
                baseHeights,
                true,
                true
        );

        assertEquals(Math.round(2400f * 0.42f), result.getTargetHeightPx());
        assertEquals(result.getTargetHeightPx(), result.getTotalHeightPx());
        assertTrue(result.getQuickBarHeightPx() > 38);
        assertTrue(result.getNumberRowHeightPx() > 46);
    }

    @Test
    public void calculate_quickBarHidden_redistributesIntoMainRows() {
        KeyboardHeightCalculator.Result result = KeyboardHeightCalculator.calculate(
                1200,
                false,
                0.42f,
                0.55f,
                300,
                4300,
                12,
                baseHeights,
                false,
                true
        );

        assertEquals(Math.round(1200f * 0.55f), result.getTargetHeightPx());
        assertEquals(result.getTargetHeightPx(), result.getTotalHeightPx());
        assertEquals(0, result.getQuickBarHeightPx());
        assertTrue(result.getRow1HeightPx() > 0);
        assertTrue(result.getRow4HeightPx() > 0);
    }

    @Test
    public void calculate_clampsToMinHeight() {
        KeyboardHeightCalculator.Result result = KeyboardHeightCalculator.calculate(
                600,
                true,
                0.42f,
                0.55f,
                400,
                900,
                12,
                baseHeights,
                true,
                true
        );

        assertEquals(400, result.getTargetHeightPx());
        assertEquals(400, result.getTotalHeightPx());
    }

    @Test
    public void calculate_clampsToMaxHeight() {
        KeyboardHeightCalculator.Result result = KeyboardHeightCalculator.calculate(
                5000,
                true,
                0.42f,
                0.55f,
                300,
                1200,
                12,
                baseHeights,
                true,
                true
        );

        assertEquals(1200, result.getTargetHeightPx());
        assertEquals(1200, result.getTotalHeightPx());
    }

    @Test
    public void calculate_sameInputProducesSameResult() {
        KeyboardHeightCalculator.Result first = KeyboardHeightCalculator.calculate(
                2400,
                true,
                0.42f,
                0.55f,
                300,
                4300,
                12,
                baseHeights,
                true,
                false
        );

        KeyboardHeightCalculator.Result second = KeyboardHeightCalculator.calculate(
                2400,
                true,
                0.42f,
                0.55f,
                300,
                4300,
                12,
                baseHeights,
                true,
                false
        );

        assertEquals(first.getQuickBarHeightPx(), second.getQuickBarHeightPx());
        assertEquals(first.getNumberRowHeightPx(), second.getNumberRowHeightPx());
        assertEquals(first.getRow1HeightPx(), second.getRow1HeightPx());
        assertEquals(first.getRow2HeightPx(), second.getRow2HeightPx());
        assertEquals(first.getRow3HeightPx(), second.getRow3HeightPx());
        assertEquals(first.getRow4HeightPx(), second.getRow4HeightPx());
        assertEquals(first.getTotalHeightPx(), second.getTotalHeightPx());
    }
}
