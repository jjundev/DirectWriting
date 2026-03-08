package com.directwriting.ime;

/**
 * Geometry helpers for stroke hit-testing.
 */
final class StrokeHitTestUtils {

    private StrokeHitTestUtils() {
    }

    static boolean isPointNearSegment(
            float pointX,
            float pointY,
            float segmentStartX,
            float segmentStartY,
            float segmentEndX,
            float segmentEndY,
            float radius
    ) {
        if (radius < 0f) {
            return false;
        }
        float radiusSquared = radius * radius;
        return distancePointToSegmentSquared(
                pointX,
                pointY,
                segmentStartX,
                segmentStartY,
                segmentEndX,
                segmentEndY
        ) <= radiusSquared;
    }

    static float distancePointToSegmentSquared(
            float pointX,
            float pointY,
            float segmentStartX,
            float segmentStartY,
            float segmentEndX,
            float segmentEndY
    ) {
        float segmentDx = segmentEndX - segmentStartX;
        float segmentDy = segmentEndY - segmentStartY;
        float segmentLengthSquared = segmentDx * segmentDx + segmentDy * segmentDy;
        if (segmentLengthSquared <= 0f) {
            return distanceSquared(pointX, pointY, segmentStartX, segmentStartY);
        }

        float projected = ((pointX - segmentStartX) * segmentDx + (pointY - segmentStartY) * segmentDy)
                / segmentLengthSquared;
        float clampedProjection = Math.max(0f, Math.min(1f, projected));
        float nearestX = segmentStartX + clampedProjection * segmentDx;
        float nearestY = segmentStartY + clampedProjection * segmentDy;
        return distanceSquared(pointX, pointY, nearestX, nearestY);
    }

    private static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}
