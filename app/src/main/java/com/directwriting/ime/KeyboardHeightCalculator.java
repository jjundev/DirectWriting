package com.directwriting.ime;

/**
 * Computes keyboard row heights from screen height ratio without cumulative scaling.
 */
public final class KeyboardHeightCalculator {

    private KeyboardHeightCalculator() {
    }

    public static Result calculate(
            int screenHeightPx,
            boolean isPortrait,
            float portraitRatio,
            float landscapeRatio,
            int minHeightPx,
            int maxHeightPx,
            int fixedExtraHeightPx,
            BaseHeights base,
            boolean quickBarVisible,
            boolean numberRowVisible
    ) {
        if (base == null) {
            throw new IllegalArgumentException("base must not be null");
        }

        float ratio = isPortrait ? portraitRatio : landscapeRatio;
        if (ratio <= 0f) {
            ratio = isPortrait ? 0.42f : 0.55f;
        }

        int rawTarget = Math.round(Math.max(0, screenHeightPx) * ratio);
        int targetHeight = clamp(rawTarget, minHeightPx, maxHeightPx);

        int scalableTarget = Math.max(1, targetHeight - Math.max(0, fixedExtraHeightPx));
        int baseScalable = Math.max(1, base.getScalableHeightPx(quickBarVisible, numberRowVisible));
        float scale = (float) scalableTarget / (float) baseScalable;

        int quickBar = quickBarVisible ? Math.max(1, Math.round(base.quickBarHeightPx * scale)) : 0;
        int numberRow = numberRowVisible ? Math.max(1, Math.round(base.numberRowHeightPx * scale)) : 0;
        int row1 = Math.max(1, Math.round(base.row1HeightPx * scale));
        int row2 = Math.max(1, Math.round(base.row2HeightPx * scale));
        int row3 = Math.max(1, Math.round(base.row3HeightPx * scale));
        int row4 = Math.max(1, Math.round(base.row4HeightPx * scale));

        int total = Math.max(0, fixedExtraHeightPx) + quickBar + numberRow + row1 + row2 + row3 + row4;
        int delta = targetHeight - total;
        row4 = Math.max(1, row4 + delta);

        int adjustedTotal = Math.max(0, fixedExtraHeightPx) + quickBar + numberRow + row1 + row2 + row3 + row4;

        return new Result(targetHeight, adjustedTotal, quickBar, numberRow, row1, row2, row3, row4);
    }

    private static int clamp(int value, int minValue, int maxValue) {
        int min = Math.max(0, minValue);
        int max = Math.max(min, maxValue);
        return Math.min(max, Math.max(min, value));
    }

    public static final class BaseHeights {
        private final int quickBarHeightPx;
        private final int numberRowHeightPx;
        private final int row1HeightPx;
        private final int row2HeightPx;
        private final int row3HeightPx;
        private final int row4HeightPx;

        public BaseHeights(
                int quickBarHeightPx,
                int numberRowHeightPx,
                int row1HeightPx,
                int row2HeightPx,
                int row3HeightPx,
                int row4HeightPx
        ) {
            this.quickBarHeightPx = Math.max(1, quickBarHeightPx);
            this.numberRowHeightPx = Math.max(1, numberRowHeightPx);
            this.row1HeightPx = Math.max(1, row1HeightPx);
            this.row2HeightPx = Math.max(1, row2HeightPx);
            this.row3HeightPx = Math.max(1, row3HeightPx);
            this.row4HeightPx = Math.max(1, row4HeightPx);
        }

        int getScalableHeightPx(boolean quickBarVisible, boolean numberRowVisible) {
            int total = row1HeightPx + row2HeightPx + row3HeightPx + row4HeightPx;
            if (quickBarVisible) {
                total += quickBarHeightPx;
            }
            if (numberRowVisible) {
                total += numberRowHeightPx;
            }
            return total;
        }
    }

    public static final class Result {
        private final int targetHeightPx;
        private final int totalHeightPx;
        private final int quickBarHeightPx;
        private final int numberRowHeightPx;
        private final int row1HeightPx;
        private final int row2HeightPx;
        private final int row3HeightPx;
        private final int row4HeightPx;

        Result(
                int targetHeightPx,
                int totalHeightPx,
                int quickBarHeightPx,
                int numberRowHeightPx,
                int row1HeightPx,
                int row2HeightPx,
                int row3HeightPx,
                int row4HeightPx
        ) {
            this.targetHeightPx = targetHeightPx;
            this.totalHeightPx = totalHeightPx;
            this.quickBarHeightPx = quickBarHeightPx;
            this.numberRowHeightPx = numberRowHeightPx;
            this.row1HeightPx = row1HeightPx;
            this.row2HeightPx = row2HeightPx;
            this.row3HeightPx = row3HeightPx;
            this.row4HeightPx = row4HeightPx;
        }

        public int getTargetHeightPx() {
            return targetHeightPx;
        }

        public int getTotalHeightPx() {
            return totalHeightPx;
        }

        public int getQuickBarHeightPx() {
            return quickBarHeightPx;
        }

        public int getNumberRowHeightPx() {
            return numberRowHeightPx;
        }

        public int getRow1HeightPx() {
            return row1HeightPx;
        }

        public int getRow2HeightPx() {
            return row2HeightPx;
        }

        public int getRow3HeightPx() {
            return row3HeightPx;
        }

        public int getRow4HeightPx() {
            return row4HeightPx;
        }
    }
}
