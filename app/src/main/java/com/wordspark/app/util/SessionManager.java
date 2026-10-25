package com.wordspark.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.wordspark.app.model.UserSettings;

/**
 * Tracks which user is currently logged in for the active app session.
 * Every personalized query (Favorites, History) must go through
 * {@link #getCurrentUserId(Context)} so that one user never sees another
 * user's data after a logout/login switch.
 */
public class SessionManager {

    private static final String PREFS_NAME = "wordspark_session";
    private static final String KEY_USER_ID = "logged_in_user_id";
    private static final String KEY_USER_NAME = "logged_in_user_name";
    private static final String KEY_THEME = "cached_theme"; // "light" | "dark" | "system"
    private static final int NO_USER = -1;

    public static void login(Context context, int userId, String userName) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.apply();
    }

    public static void logout(Context context) {
        // Clears the session AND resets the theme back to light default, so the next
        // person to open Login/Register sees light theme rather than the
        // previous user's preference.
        prefs(context).edit().clear().apply();
        setCachedTheme(context, UserSettings.THEME_LIGHT);
        ThemeUtil.apply(UserSettings.THEME_LIGHT);
    }

    public static boolean isLoggedIn(Context context) {
        return getCurrentUserId(context) != NO_USER;
    }

    public static int getCurrentUserId(Context context) {
        return prefs(context).getInt(KEY_USER_ID, NO_USER);
    }

    public static String getCurrentUserName(Context context) {
        return prefs(context).getString(KEY_USER_NAME, "");
    }

    /** Fast, synchronous read used by BaseActivity before setContentView(). Defaults to "light". */
    public static String getCachedTheme(Context context) {
        return prefs(context).getString(KEY_THEME, UserSettings.THEME_LIGHT);
    }

    /** Updates the cache; call this right after loading/saving a user's theme in SQLite. */
    public static void setCachedTheme(Context context, String theme) {
        prefs(context).edit().putString(KEY_THEME, theme == null ? UserSettings.THEME_LIGHT : theme).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
