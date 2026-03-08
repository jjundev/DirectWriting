package com.directwriting.ime;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrokeHitTestUtilsTest {

    @Test
    public void isPointNearSegment_horizontalSegment_insideRadius() {
        boolean hit = StrokeHitTestUtils.isPointNearSegment(
                5f,
                2f,
                0f,
                0f,
                10f,
                0f,
                2f
        );
        assertTrue(hit);
    }

    @Test
    public void isPointNearSegment_verticalSegment_outsideRadius() {
        boolean hit = StrokeHitTestUtils.isPointNearSegment(
                3.1f,
                5f,
                0f,
                0f,
                0f,
                10f,
                3f
        );
        assertFalse(hit);
    }

    @Test
    public void isPointNearSegment_diagonalSegment_boundaryIncluded() {
        boolean hit = StrokeHitTestUtils.isPointNearSegment(
                5f,
                7f,
                0f,
                0f,
                10f,
                10f,
                2f
        );
        assertTrue(hit);
    }

    @Test
    public void isPointNearSegment_zeroLengthSegment_usesPointDistance() {
        boolean near = StrokeHitTestUtils.isPointNearSegment(
                2f,
                1f,
                0f,
                0f,
                0f,
                0f,
                2.24f
        );
        boolean far = StrokeHitTestUtils.isPointNearSegment(
                2f,
                1f,
                0f,
                0f,
                0f,
                0f,
                2.23f
        );
        assertTrue(near);
        assertFalse(far);
    }
}
