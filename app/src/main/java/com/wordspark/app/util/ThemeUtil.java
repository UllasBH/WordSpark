package com.wordspark.app.util;

import androidx.appcompat.app.AppCompatDelegate;

import com.wordspark.app.model.UserSettings;

public class ThemeUtil {

    /** Applies the given theme ("light"/"dark"/"system") app-wide via AppCompatDelegate. Default is Light. */
    public static void apply(String theme) {
        if (UserSettings.THEME_DARK.equalsIgnoreCase(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
