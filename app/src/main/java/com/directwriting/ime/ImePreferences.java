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

    public static final int DEFAULT_PEN_THICKNESS = 4;
    public static final boolean DEFAULT_PRESSURE_SENSITIVITY = true;

    private ImePreferences() {
    }

    public static int getPenThickness(Context context) {
        return getPrefs(context).getInt(KEY_PEN_THICKNESS, DEFAULT_PEN_THICKNESS);
    }

    public static void setPenThickness(Context context, int value) {
        getPrefs(context).edit().putInt(KEY_PEN_THICKNESS, Math.max(1, value)).apply();
    }

    public static boolean isPressureSensitivityEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_PRESSURE_SENSITIVITY, DEFAULT_PRESSURE_SENSITIVITY);
    }

    public static void setPressureSensitivityEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_PRESSURE_SENSITIVITY, enabled).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
