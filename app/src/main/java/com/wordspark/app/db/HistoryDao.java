package com.wordspark.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.wordspark.app.model.HistoryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Data access for the "history" table.
 * Every method requires a userId so that a user can only ever read or write
 * their own search history.
 *
 * Re-searching a word that is already in the user's history updates its
 * timestamp (bumping it to the top) instead of inserting a duplicate row.
 */
public class HistoryDao {

    private final DatabaseHelper dbHelper;

    public HistoryDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /** Records a search with no retention-limit enforcement (limit enforced separately if needed). */
    public void recordSearch(int userId, String word) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_HIST_USER_ID, userId);
        values.put(DatabaseHelper.COL_HIST_WORD, word.toLowerCase(Locale.ROOT));
        values.put(DatabaseHelper.COL_HIST_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(DatabaseHelper.TABLE_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Records a search, then immediately trims the user's history down to
     * their configured retention limit (oldest entries removed first) so
     * the table never grows past what Settings > History Limit allows.
     */
    public void recordSearch(int userId, String word, int retentionLimit) {
        recordSearch(userId, word);
        enforceRetentionLimit(userId, retentionLimit);
    }

    /**
     * Trims this user's history down to retentionLimit entries, removing the
     * oldest first. Called both right after a new search, and immediately
     * when the user lowers their History Limit in Settings (so the new
     * limit is respected right away, not just after future searches).
     */
    public void enforceRetentionLimit(int userId, int retentionLimit) {
        if (retentionLimit <= 0) return; // 0/negative = no enforcement
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_HISTORY +
                " WHERE " + DatabaseHelper.COL_HIST_USER_ID + " = ? AND " + DatabaseHelper.COL_HIST_ID + " NOT IN (" +
                "SELECT " + DatabaseHelper.COL_HIST_ID + " FROM " + DatabaseHelper.TABLE_HISTORY +
                " WHERE " + DatabaseHelper.COL_HIST_USER_ID + " = ? " +
                " ORDER BY " + DatabaseHelper.COL_HIST_TIMESTAMP + " DESC LIMIT ?)",
                new Object[]{userId, userId, retentionLimit});
    }

    public List<HistoryEntry> getHistory(int userId) {
        return getHistory(userId, -1);
    }

    public List<HistoryEntry> getHistory(int userId, int limit) {
        List<HistoryEntry> results = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String limitClause = limit > 0 ? String.valueOf(limit) : null;

        Cursor cursor = db.query(DatabaseHelper.TABLE_HISTORY,
                null,
                DatabaseHelper.COL_HIST_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                DatabaseHelper.COL_HIST_TIMESTAMP + " DESC",
                limitClause);

        while (cursor.moveToNext()) {
            results.add(new HistoryEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_WORD)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HIST_TIMESTAMP))
            ));
        }
        cursor.close();
        return results;
    }

    public void clearHistory(int userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_HISTORY, DatabaseHelper.COL_HIST_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
    }
}
