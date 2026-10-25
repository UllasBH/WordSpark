package com.wordspark.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wordspark.app.model.UserSettings;

/**
 * Data access for the "user_settings" table: one row per user, holding
 * theme, history limit, notification preferences, quiz best score,
 * quiz attempts, total correct, and flashcards reviewed.
 */
public class SettingsDao {

    private final DatabaseHelper dbHelper;

    public SettingsDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /** Returns this user's settings, creating a default row on first access. */
    public UserSettings getSettings(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SETTINGS, null,
                DatabaseHelper.COL_SET_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, null);

        UserSettings settings = new UserSettings();
        if (cursor.moveToFirst()) {
            settings.setTheme(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SET_THEME)));
            settings.setHistoryLimit(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SET_HISTORY_LIMIT)));
            settings.setNotificationsEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SET_NOTIFICATIONS_ENABLED)) == 1);
            settings.setNotificationTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SET_NOTIFICATION_TIME)));
            settings.setQuizBestScore(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SET_QUIZ_BEST_SCORE)));
            settings.setQuizBestTotal(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SET_QUIZ_BEST_TOTAL)));

            int attemptsCol = cursor.getColumnIndex(DatabaseHelper.COL_SET_QUIZ_ATTEMPTS);
            if (attemptsCol != -1) settings.setQuizAttempts(cursor.getInt(attemptsCol));
            int correctCol = cursor.getColumnIndex(DatabaseHelper.COL_SET_QUIZ_TOTAL_CORRECT);
            if (correctCol != -1) settings.setQuizTotalCorrect(cursor.getInt(correctCol));
            int flashCol = cursor.getColumnIndex(DatabaseHelper.COL_SET_FLASHCARDS_REVIEWED);
            if (flashCol != -1) settings.setFlashcardsReviewed(cursor.getInt(flashCol));

            cursor.close();
        } else {
            cursor.close();
            insertDefaultRow(userId, settings);
        }
        return settings;
    }

    private void insertDefaultRow(int userId, UserSettings defaults) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SET_USER_ID, userId);
        values.put(DatabaseHelper.COL_SET_THEME, defaults.getTheme());
        values.put(DatabaseHelper.COL_SET_HISTORY_LIMIT, defaults.getHistoryLimit());
        values.put(DatabaseHelper.COL_SET_NOTIFICATIONS_ENABLED, defaults.isNotificationsEnabled() ? 1 : 0);
        values.put(DatabaseHelper.COL_SET_NOTIFICATION_TIME, defaults.getNotificationTime());
        values.put(DatabaseHelper.COL_SET_QUIZ_BEST_SCORE, defaults.getQuizBestScore());
        values.put(DatabaseHelper.COL_SET_QUIZ_BEST_TOTAL, defaults.getQuizBestTotal());
        values.put(DatabaseHelper.COL_SET_QUIZ_ATTEMPTS, defaults.getQuizAttempts());
        values.put(DatabaseHelper.COL_SET_QUIZ_TOTAL_CORRECT, defaults.getQuizTotalCorrect());
        values.put(DatabaseHelper.COL_SET_FLASHCARDS_REVIEWED, defaults.getFlashcardsReviewed());
        db.insertWithOnConflict(DatabaseHelper.TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void setTheme(int userId, String theme) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SET_THEME, theme);
        upsert(userId, values);
    }

    public void setHistoryLimit(int userId, int limit) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SET_HISTORY_LIMIT, limit);
        upsert(userId, values);
    }

    public void setNotificationsEnabled(int userId, boolean enabled) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SET_NOTIFICATIONS_ENABLED, enabled ? 1 : 0);
        upsert(userId, values);
    }

    public void setNotificationTime(int userId, String time) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SET_NOTIFICATION_TIME, time);
        upsert(userId, values);
    }

    public void recordQuizResult(int userId, int correct, int total) {
        UserSettings current = getSettings(userId);
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SET_QUIZ_ATTEMPTS, current.getQuizAttempts() + 1);
        values.put(DatabaseHelper.COL_SET_QUIZ_TOTAL_CORRECT, current.getQuizTotalCorrect() + correct);
        if (!current.hasQuizBestScore() || correct > current.getQuizBestScore()) {
            values.put(DatabaseHelper.COL_SET_QUIZ_BEST_SCORE, correct);
            values.put(DatabaseHelper.COL_SET_QUIZ_BEST_TOTAL, total);
        }
        upsert(userId, values);
    }

    public void incrementFlashcardsReviewed(int userId) {
        UserSettings current = getSettings(userId);
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SET_FLASHCARDS_REVIEWED, current.getFlashcardsReviewed() + 1);
        upsert(userId, values);
    }

    public boolean updateQuizBestScoreIfHigher(int userId, int correct, int total) {
        recordQuizResult(userId, correct, total);
        return true;
    }

    /** Ensures a settings row exists, then applies a partial update to it. */
    private void upsert(int userId, ContentValues partialValues) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.update(DatabaseHelper.TABLE_SETTINGS, partialValues,
                DatabaseHelper.COL_SET_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        if (rows == 0) {
            insertDefaultRow(userId, new UserSettings());
            db.update(DatabaseHelper.TABLE_SETTINGS, partialValues,
                    DatabaseHelper.COL_SET_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        }
    }
}
