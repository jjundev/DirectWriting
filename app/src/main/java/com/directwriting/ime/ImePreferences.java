package com.directwriting.ime;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences helper for IME settings.
 */
public final class ImePreferences {

    private static final String PREFS_NAME = "direct_writing_prefs";
    private static final String KEY_PEN_THICKNESS = "pen_thickness";
    private static final String KEY_PRESSURE_SENSITIVITY = "pressure_sensitivity";
    private static final String KEY_PEN_COLOR = "pen_color";
    private static final String KEY_ERASER_MODE = "eraser_mode";
    private static final String KEY_ERASER_SIZE = "eraser_size";

    public static final int MIN_PEN_THICKNESS = 1;
    public static final int MAX_PEN_THICKNESS = 20;
    public static final int DEFAULT_PEN_THICKNESS = 4;
    public static final boolean DEFAULT_PRESSURE_SENSITIVITY = true;
    public static final int DEFAULT_PEN_COLOR = 0xFF111111;

    public static final int ERASER_MODE_STROKE = 0;
    public static final int ERASER_MODE_AREA = 1;
    public static final int DEFAULT_ERASER_MODE = ERASER_MODE_STROKE;
    public static final int MIN_ERASER_SIZE = 8;
    public static final int MAX_ERASER_SIZE = 72;
    public static final int DEFAULT_ERASER_SIZE = 24;

    private ImePreferences() {
    }

    public static int getPenThickness(Context context) {
        return clampPenThickness(getPrefs(context).getInt(KEY_PEN_THICKNESS, DEFAULT_PEN_THICKNESS));
    }

    public static void setPenThickness(Context context, int value) {
        getPrefs(context).edit().putInt(KEY_PEN_THICKNESS, clampPenThickness(value)).apply();
    }

    public static boolean isPressureSensitivityEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_PRESSURE_SENSITIVITY, DEFAULT_PRESSURE_SENSITIVITY);
    }

    public static void setPressureSensitivityEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_PRESSURE_SENSITIVITY, enabled).apply();
    }

    public static int getPenColor(Context context) {
        return getPrefs(context).getInt(KEY_PEN_COLOR, DEFAULT_PEN_COLOR);
    }

    public static void setPenColor(Context context, int color) {
        getPrefs(context).edit().putInt(KEY_PEN_COLOR, color).apply();
    }

    public static int getEraserMode(Context context) {
        int mode = getPrefs(context).getInt(KEY_ERASER_MODE, DEFAULT_ERASER_MODE);
        return normalizeEraserMode(mode);
    }

    public static void setEraserMode(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_ERASER_MODE, normalizeEraserMode(mode)).apply();
    }

    public static int getEraserSize(Context context) {
        return clampEraserSize(getPrefs(context).getInt(KEY_ERASER_SIZE, DEFAULT_ERASER_SIZE));
    }

    public static void setEraserSize(Context context, int sizePx) {
        getPrefs(context).edit().putInt(KEY_ERASER_SIZE, clampEraserSize(sizePx)).apply();
    }

    static int clampPenThickness(int value) {
        return Math.max(MIN_PEN_THICKNESS, Math.min(value, MAX_PEN_THICKNESS));
    }

    static int clampEraserSize(int value) {
        return Math.max(MIN_ERASER_SIZE, Math.min(value, MAX_ERASER_SIZE));
    }

    static int normalizeEraserMode(int mode) {
        return mode == ERASER_MODE_AREA ? ERASER_MODE_AREA : ERASER_MODE_STROKE;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
