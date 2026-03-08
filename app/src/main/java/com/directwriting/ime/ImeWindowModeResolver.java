package com.directwriting.ime;

import android.content.res.Configuration;

/**
 * Resolves tablet/fullscreen/split-keyboard mode decisions from current configuration values.
 */
final class ImeWindowModeResolver {

    private static final int TABLET_MIN_SMALLEST_WIDTH_DP = 600;
    private static final float SPLIT_GAP_RATIO = 0.26f;

    private ImeWindowModeResolver() {
    }

    static boolean isTablet(int smallestScreenWidthDp) {
        return smallestScreenWidthDp >= TABLET_MIN_SMALLEST_WIDTH_DP;
    }

    static boolean shouldDisableExtractUi(int smallestScreenWidthDp) {
        return isTablet(smallestScreenWidthDp);
    }

    static boolean shouldUseSplitLayout(int smallestScreenWidthDp, int orientation) {
        return isTablet(smallestScreenWidthDp)
                && orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    static int resolveSplitGapWidthPx(int rowWidthPx) {
        return Math.round(Math.max(0, rowWidthPx) * SPLIT_GAP_RATIO);
    }
}
